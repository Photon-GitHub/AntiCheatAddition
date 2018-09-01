package de.photon.AACAdditionPro.util.fakeentity;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.api.killauraentity.Movement;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.entity.EntityUtil;
import de.photon.AACAdditionPro.util.fakeentity.movement.Collision;
import de.photon.AACAdditionPro.util.fakeentity.movement.Gravitation;
import de.photon.AACAdditionPro.util.fakeentity.movement.Jumping;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.mathematics.RotationUtil;
import de.photon.AACAdditionPro.util.packetwrappers.IWrapperPlayClientOnGround;
import de.photon.AACAdditionPro.util.packetwrappers.IWrapperPlayServerEntityLook;
import de.photon.AACAdditionPro.util.packetwrappers.IWrapperPlayServerRelEntityMove;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerAnimation;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntity;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityDestroy;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityHeadRotation;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityLook;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityMetadata;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityTeleport;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerRelEntityMove;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerRelEntityMoveLook;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Objects;

public abstract class ClientsideLivingEntity
{
    @Getter
    protected final int entityID;

    /**
     * Determines whether this {@link ClientsideLivingEntity} is already spawned.
     */
    @Getter
    private boolean spawned;

    /**
     * Determines whether this {@link ClientsideLivingEntity} should tp in the next move, ignoring all other calculations.
     */
    @Setter
    private boolean needsTeleport;

    /**
     * The current velocity of this {@link ClientsideLivingEntity}.
     */
    protected Vector velocity = new Vector(0, 0, 0);

    protected Location lastLocation;
    protected Location location;
    private boolean onGround;

    @Getter
    protected float lastHeadYaw;
    @Getter
    protected float headYaw;

    private boolean sprinting;

    @Getter
    protected final Player observedPlayer;

    @Getter
    protected final Hitbox hitbox;

    @Getter
    private boolean visible = true;

    @Getter
    private long ticksExisted = 0;
    private int tickTask = -1;

    // Movement state machine
    @Setter
    private Movement currentMovementCalculator;

    /**
     * Constructs a new {@link ClientsideLivingEntity}.
     *
     * @param observedPlayer the player that should see this {@link ClientsideLivingEntity}
     * @param hitbox         the {@link Hitbox} of this {@link ClientsideLivingEntity}
     * @param movement       the {@link Movement} of this {@link ClientsideLivingEntity}.
     */
    public ClientsideLivingEntity(final Player observedPlayer, final Hitbox hitbox, final Movement movement)
    {
        this.observedPlayer = observedPlayer;
        this.hitbox = hitbox;

        // Set default movement state
        this.currentMovementCalculator = movement;

        tickTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), this::tick, 1L, 1L);

        // Get a valid entity ID
        this.entityID = EntityIdUtil.getNextEntityID();
    }

    // --------------------------------------------------------------- General -------------------------------------------------------------- //

    /**
     * This is used to check if this {@link ClientsideLivingEntity} is attached to an {@link User} and therefore valid
     *
     * @return true if this {@link ClientsideLivingEntity} is attached to an {@link User}, false otherwise
     */
    public boolean isValid()
    {
        final User user = UserManager.getUser(observedPlayer.getUniqueId());

        return !User.isUserInvalid(user, ModuleType.KILLAURA_ENTITY) &&
               user.getClientSideEntityData().clientSidePlayerEntity != null &&
               user.getClientSideEntityData().clientSidePlayerEntity.getEntityID() == this.entityID;
    }

    /**
     * Should be called every tick once, updates physics + sends movement packets
     */
    protected void tick()
    {
        // TicksExisted
        ticksExisted++;

        // ------------------------------------------ Movement system -----------------------------------------------//
        // Get the next position and move
        final Location moveToLocation = Objects.requireNonNull(this.currentMovementCalculator.calculate(this.observedPlayer.getLocation(), this.location.clone()), "Movement did not calculate a valid location.");

        if (this.currentMovementCalculator.isTPNeeded())
        {
            this.needsTeleport = true;
        }

        // ------------------------------------------ Velocity system -----------------------------------------------//

        if (this.needsTeleport)
        {
            this.move(Collision.getClosestFreeSpaceYAxis(moveToLocation, this.hitbox));

            // Velocity reset on teleport.
            this.velocity.zero();
            this.needsTeleport = false;
        }
        else
        {
            // Only set the x- and the z- axis (y should be handled by autojumping).
            final Location moveVelocity = moveToLocation.subtract(this.location);
            this.velocity.setX(moveVelocity.getX()).setZ(moveVelocity.getZ());

            final Vector collidedVelocity = Collision.getNearestUncollidedLocation(this.observedPlayer, this.location, this.hitbox, this.velocity);
            this.location = this.location.add(collidedVelocity);

            // Already added the velocity to location and collided it
            // ClientCopy
            this.onGround = (collidedVelocity.getY() != this.velocity.getY()) &&
                            // Due to gravity a player always have a negative velocity if walking/running on the ground.
                            velocity.getY() <= 0 &&
                            // Make sure the entity only jumps on real blocks, not e.g. grass.
                            this.location.clone().subtract(0, 0.05, 0).getBlock().getType().isSolid();

            this.sprinting = this.currentMovementCalculator.shouldSprint();

            // Calculate velocity
            if (this.onGround)
            {
                // After a certain period the entity might reach a velocity so high that it appears to be "glitching"
                // through the ground. This can be prevented by resetting the velocity if the entity is onGround.
                this.velocity.setY(0);
            }
            else
            {
                this.velocity = Gravitation.applyGravitationAndAirResistance(this.velocity, Gravitation.PLAYER);
            }

            final Vector tempJumpVelocity = this.velocity.clone();
            tempJumpVelocity.setX(Math.signum(tempJumpVelocity.getX()));
            tempJumpVelocity.setY(Jumping.getJumpYMotion(null));
            tempJumpVelocity.setZ(Math.signum(tempJumpVelocity.getZ()));

            // Whether the entity should jump if horizontally collided
            if (this.currentMovementCalculator.jumpIfCollidedHorizontally() &&
                // Check whether the entity can really jump to that location.
                EntityUtil.getMaterialsInHitbox(this.location.clone().add(tempJumpVelocity), this.hitbox).stream().noneMatch(material -> material != Material.AIR))
            {
                this.jump();
            }
        }

        sendMove();
        sendHeadYaw();
    }

    // -------------------------------------------------------------- Movement ------------------------------------------------------------ //

    /**
     * Moves the {@link ClientsideLivingEntity} somewhere
     *
     * @param location the target {@link Location} this {@link ClientsideLivingEntity} should be moved to.
     */
    public void move(Location location)
    {
        this.location = location.clone();
    }

    /**
     * sends a fitting movement to from the last location to the current location
     */
    private void sendMove()
    {
        if (!this.spawned)
        {
            return;
        }

        final Vector relativeLocation = this.location.toVector().subtract(this.lastLocation.toVector());

        final boolean savedOnGround = this.onGround;

        // Teleport needed ?
        final int teleportThreshold;
        // Do not use the client version here.
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                teleportThreshold = 4;
                break;
            case MC111:
            case MC112:
            case MC113:
                teleportThreshold = 8;
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }

        if (Math.abs(relativeLocation.getX()) + Math.abs(relativeLocation.getY()) + Math.abs(relativeLocation.getZ()) > teleportThreshold || needsTeleport)
        {
            final WrapperPlayServerEntityTeleport teleportWrapper = new WrapperPlayServerEntityTeleport();
            // Position + Angle
            teleportWrapper.setWithLocation(this.location);
            // OnGround
            teleportWrapper.setOnGround(savedOnGround);
            // Send the packet
            teleportWrapper.sendPacket(this.observedPlayer);
            this.needsTeleport = false;
            // System.out.println("Sent TP to: " + this.location.getX() + " | " + this.location.getY() + " | " + this.location.getZ());
        }
        else
        {
            // Sending relative movement
            final boolean move = relativeLocation.lengthSquared() != 0;
            final boolean look = this.location.getPitch() != this.lastLocation.getPitch() || this.location.getYaw() != this.lastLocation.getYaw();

            // Get the correct packet.
            final WrapperPlayServerEntity packetWrapper = move ?
                                                          (look ?
                                                           new WrapperPlayServerRelEntityMoveLook() :
                                                           new WrapperPlayServerRelEntityMove()) :
                                                          (look ?
                                                           new WrapperPlayServerEntityLook() :
                                                           new WrapperPlayServerEntity());

            if (packetWrapper instanceof IWrapperPlayClientOnGround)
            {
                final IWrapperPlayClientOnGround onGroundWrapper = (IWrapperPlayClientOnGround) packetWrapper;
                onGroundWrapper.setOnGround(savedOnGround);

                if (onGroundWrapper instanceof IWrapperPlayServerEntityLook)
                {
                    final IWrapperPlayServerEntityLook lookWrapper = (IWrapperPlayServerEntityLook) onGroundWrapper;
                    lookWrapper.setYaw(this.location.getYaw());
                    lookWrapper.setPitch(this.location.getPitch());
                }

                if (onGroundWrapper instanceof IWrapperPlayServerRelEntityMove)
                {
                    final IWrapperPlayServerRelEntityMove relMoveWrapper = (IWrapperPlayServerRelEntityMove) onGroundWrapper;
                    relMoveWrapper.setDiffs(relativeLocation);
                }
            }

            packetWrapper.setEntityID(this.entityID);
            packetWrapper.sendPacket(this.observedPlayer);
        }

        this.lastLocation = this.location.clone();
    }

    public void jump()
    {
        if (this.onGround)
        {
            velocity.setY(Jumping.getJumpYMotion(null));

            if (this.sprinting)
            {
                velocity.add(location.getDirection().setY(0).normalize().multiply(.2F));
            }
        }
    }

    private void sendHeadYaw()
    {
        // The entity is already spawned
        if (this.isSpawned() &&
            // and has moved it's head.
            this.headYaw != this.lastHeadYaw)
        {
            final WrapperPlayServerEntityHeadRotation headRotationWrapper = new WrapperPlayServerEntityHeadRotation();

            headRotationWrapper.setEntityID(entityID);
            headRotationWrapper.setHeadYaw(RotationUtil.getFixRotation(headYaw));

            headRotationWrapper.sendPacket(observedPlayer);
            lastHeadYaw = headYaw;
        }
    }


    public Location getLocation()
    {
        return location == null ?
               null :
               location.clone();
    }

    public Location getLastLocation()
    {
        return lastLocation == null ?
               null :
               lastLocation.clone();
    }

    public Vector getVelocity()
    {
        return velocity.clone();
    }

    public void setVelocity(Vector velocity)
    {
        this.velocity = velocity.clone();
    }

    public Movement getMovement()
    {
        return this.currentMovementCalculator;
    }

    /**
     * Calculates a valid {@link Location} to teleport this {@link ClientsideLivingEntity} to.
     */
    public Location calculateTeleportLocation()
    {
        return Collision.getClosestFreeSpaceYAxis(this.currentMovementCalculator.calculate(observedPlayer.getLocation(), observedPlayer.getLocation()), this.getHitbox());
    }

    // -------------------------------------------------------------- Simulation ------------------------------------------------------------ //

    /**
     * Fakes the swing - animation to make the entity look like it is a real, fighting {@link Player}.
     */
    public void swing()
    {
        this.fakeAnimation(0);
    }

    /**
     * Used to apply the visibility effect to the entity and remove it once again.
     */
    public void setVisibility(final boolean visible)
    {
        // Do we need to change something?
        if (this.visible != visible)
        {
            final WrapperPlayServerEntityMetadata entityMetadataWrapper = new WrapperPlayServerEntityMetadata();
            entityMetadataWrapper.setEntityID(this.entityID);

            entityMetadataWrapper.setMetadata(entityMetadataWrapper.builder()
                                                                   // Invisibility
                                                                   .setZeroIndex((byte) (visible ? 0 : 0x20))
                                                                   // Arrows in entity.
                                                                   .setArrowInEntityMetadata(0)
                                                                   .asList());

            entityMetadataWrapper.sendPacket(this.observedPlayer);

            this.visible = visible;
        }
    }

    protected void fakeAnimation(final int animationType)
    {
        // Entity is already spawned.
        if (this.isSpawned())
        {
            final WrapperPlayServerAnimation animationWrapper = new WrapperPlayServerAnimation();
            animationWrapper.setEntityID(this.entityID);
            animationWrapper.setAnimation(animationType);
            animationWrapper.sendPacket(this.observedPlayer);
        }
    }

    // ---------------------------------------------------------------- Spawn --------------------------------------------------------------- //

    public void spawn(Location location)
    {
        this.spawned = true;
    }

    // --------------------------------------------------------------- Despawn -------------------------------------------------------------- //

    public void despawn()
    {
        // Cancel tick task
        if (tickTask != -1)
        {
            Bukkit.getScheduler().cancelTask(tickTask);
            this.tickTask = -1;
        }

        if (spawned)
        {
            final WrapperPlayServerEntityDestroy entityDestroyWrapper = new WrapperPlayServerEntityDestroy();
            entityDestroyWrapper.setEntityIds(new int[]{this.entityID});
            entityDestroyWrapper.sendPacket(observedPlayer);
            this.spawned = false;
        }
    }
}

package de.photon.AACAdditionPro.util.fakeentity;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.api.killauraentity.Movement;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.fakeentity.movement.Collision;
import de.photon.AACAdditionPro.util.fakeentity.movement.Gravitation;
import de.photon.AACAdditionPro.util.fakeentity.movement.Jumping;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.mathematics.RotationUtil;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerAnimation;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntity;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityDestroy;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityHeadRotation;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityLook;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityMetadata;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityTeleport;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerRelEntityMove;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerRelEntityMoveLook;
import de.photon.AACAdditionPro.util.reflection.Reflect;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public abstract class ClientsideEntity
{
    // xz-Distance after which a teleport is forced.
    private static final Field entityCountField;

    static
    {
        entityCountField = Reflect.fromNMS("Entity").field("entityCount").getField();
        entityCountField.setAccessible(true);
    }

    @Getter
    protected final int entityID;

    /**
     * Determines whether this {@link ClientsideEntity} is already spawned.
     */
    @Getter
    private boolean spawned;

    /**
     * Determines whether this {@link ClientsideEntity} should tp in the next move, ignoring all other calculations.
     */
    @Setter
    private boolean needsTeleport;

    /**
     * Stores the last timestamp this {@link ClientsideEntity} was hit.
     */
    public long lastHurtMillis;
    private BukkitTask hurtTask = null;

    /**
     * The current velocity of this {@link ClientsideEntity}.
     */
    private Vector velocity = new Vector(0, 0, 0);

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
     * Constructs a new {@link ClientsideEntity}.
     *
     * @param observedPlayer the player that should see this {@link ClientsideEntity}
     * @param hitbox         the {@link Hitbox} of this {@link ClientsideEntity}
     * @param movement       the {@link Movement} of this {@link ClientsideEntity}.
     */
    public ClientsideEntity(final Player observedPlayer, final Hitbox hitbox, final Movement movement)
    {
        this.observedPlayer = observedPlayer;
        this.hitbox = hitbox;

        // Set default movement state
        this.currentMovementCalculator = movement;

        tickTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), this::tick, 1L, 1L);

        // Get a valid entity ID
        try
        {
            this.entityID = getNextEntityID();
        } catch (IllegalAccessException ex)
        {
            throw new RuntimeException("Could not create ClientsideEntity for player " + observedPlayer.getName(), ex);
        }
    }

    // --------------------------------------------------------------- General -------------------------------------------------------------- //

    /**
     * This is used to check if this {@link ClientsideEntity} is attached to an {@link User} and therefore valid
     *
     * @return true if this {@link ClientsideEntity} is attached to an {@link User}, false otherwise
     */
    public boolean isValid()
    {
        final User user = UserManager.getUser(observedPlayer.getUniqueId());

        return !User.isUserInvalid(user) &&
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
                BlockUtils.getMaterialsInHitbox(this.location.clone().add(tempJumpVelocity), this.hitbox).stream().noneMatch(material -> material != Material.AIR))
            {
                this.jump();
            }
        }

        sendMove();
        sendHeadYaw();
    }

    // -------------------------------------------------------------- Movement ------------------------------------------------------------ //

    /**
     * Moves the {@link ClientsideEntity} somewhere
     *
     * @param location the target {@link Location} this {@link ClientsideEntity} should be moved to.
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

        double xDiff = this.location.getX() - this.lastLocation.getX();
        double yDiff = this.location.getY() - this.lastLocation.getY();
        double zDiff = this.location.getZ() - this.lastLocation.getZ();

        final boolean savedOnGround = this.onGround;

        // Teleport needed ?
        int teleportThreshold;
        // Do not use the client version here.
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                teleportThreshold = 4;
                break;
            case MC111:
            case MC112:
                teleportThreshold = 8;
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }

        if (Math.abs(xDiff) + Math.abs(yDiff) + Math.abs(zDiff) > teleportThreshold || needsTeleport)
        {
            final WrapperPlayServerEntityTeleport teleportWrapper = new WrapperPlayServerEntityTeleport();
            // Position
            teleportWrapper.setX(this.location.getX());
            teleportWrapper.setY(this.location.getY());
            teleportWrapper.setZ(this.location.getZ());
            // Angle
            teleportWrapper.setYaw(this.location.getYaw());
            teleportWrapper.setPitch(this.location.getPitch());
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
            boolean move = xDiff != 0 || yDiff != 0 || zDiff != 0;
            boolean look = this.location.getPitch() != this.lastLocation.getPitch() || this.location.getYaw() != this.lastLocation.getYaw();

            WrapperPlayServerEntity packetWrapper;

            if (move)
            {
                WrapperPlayServerRelEntityMove movePacketWrapper;

                if (look)
                {
                    WrapperPlayServerRelEntityMoveLook moveLookPacketWrapper = new WrapperPlayServerRelEntityMoveLook();

                    // Angle
                    moveLookPacketWrapper.setYaw(this.location.getYaw());
                    moveLookPacketWrapper.setPitch(this.location.getPitch());

                    movePacketWrapper = moveLookPacketWrapper;
                    // System.out.println("Sending movelook");
                }
                else
                {
                    movePacketWrapper = new WrapperPlayServerRelEntityMove();
                    // System.out.println("Sending move");
                }

                movePacketWrapper.setOnGround(savedOnGround);
                movePacketWrapper.setDiffs(xDiff, yDiff, zDiff);
                packetWrapper = movePacketWrapper;
            }
            else if (look)
            {
                WrapperPlayServerEntityLook lookPacketWrapper = new WrapperPlayServerEntityLook();

                // Angles
                lookPacketWrapper.setYaw(this.location.getYaw());
                lookPacketWrapper.setPitch(this.location.getPitch());
                // OnGround
                lookPacketWrapper.setOnGround(savedOnGround);

                packetWrapper = lookPacketWrapper;
                // System.out.println("Sending look");

            }
            else
            {
                packetWrapper = new WrapperPlayServerEntity();
                // System.out.println("Sending idle");
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
     * Calculates a valid {@link Location} to teleport this {@link ClientsideEntity} to.
     */
    public Location calculateTeleportLocation()
    {
        return Collision.getClosestFreeSpaceYAxis(this.currentMovementCalculator.calculate(observedPlayer.getLocation(), observedPlayer.getLocation()), this.getHitbox());
    }

    // -------------------------------------------------------------- Simulation ------------------------------------------------------------ //

    /**
     * Fake being hurt by the observedPlayer in the next sync server tick
     */
    public void hurtByObserved()
    {
        hurtTask = Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
            lastHurtMillis = System.currentTimeMillis();
            hurt();

            Location observedLoc = observedPlayer.getLocation();
            observedLoc.setPitch(0);

            // Calculate knockback strength
            int knockbackStrength = observedPlayer.isSprinting() ? 1 : 0;

            final ItemStack itemInHand;
            switch (ServerVersion.getActiveServerVersion())
            {
                case MC188:
                    itemInHand = observedPlayer.getItemInHand();
                    break;
                case MC111:
                case MC112:
                    itemInHand = observedPlayer.getInventory().getItemInMainHand();
                    break;
                default:
                    throw new IllegalStateException("Unknown minecraft version");
            }

            if (itemInHand != null)
            {
                knockbackStrength += itemInHand.getEnchantmentLevel(Enchantment.KNOCKBACK);
            }

            // Apply velocity
            if (knockbackStrength > 0)
            {
                velocity.add(observedLoc.getDirection().normalize().setY(.1).multiply(knockbackStrength * .5));

                //TODO wrong code, its not applied generally, needs to be moved into the method the fake entity hits another entity and apply knockback + sprinting options
//                    motX *= 0.6D;
//                    motZ *= 0.6D;
            }
        });
    }

    /**
     * Fakes the swing - animation to make the entity look like it is a real, fighting {@link Player}.
     */
    public void swing()
    {
        this.fakeAnimation(0);
    }

    /**
     * Fakes the hurt - animation to make the entity look like it was hurt
     */
    public void hurt()
    {
        fakeAnimation(1);
    }

    /**
     * Used to apply the visibility effect to the entity and remove it once again.
     */
    public void setVisibility(final boolean visible)
    {
        if (this.visible == visible)
        {
            // No need to change anything.
            return;
        }

        final WrapperPlayServerEntityMetadata entityMetadataWrapper = new WrapperPlayServerEntityMetadata();
        entityMetadataWrapper.setEntityID(this.entityID);

        final byte visibleByte = (byte) (visible ? 0 : 0x20);
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                final List<WrappedWatchableObject> wrappedWatchableObjectsOldMC = Arrays.asList(
                        // Invisibility itself
                        new WrappedWatchableObject(0, visibleByte),
                        // Arrows in entity.
                        // IN 1.8.8 THIS IS A BYTE, NOT AN INTEGER!
                        new WrappedWatchableObject(10, (byte) 0));
                entityMetadataWrapper.setMetadata(wrappedWatchableObjectsOldMC);
                break;

            case MC111:
            case MC112:
                final WrappedDataWatcher.WrappedDataWatcherObject visibilityWatcher = new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class));
                final WrappedDataWatcher.WrappedDataWatcherObject arrowInEntityWatcher = new WrappedDataWatcher.WrappedDataWatcherObject(10, WrappedDataWatcher.Registry.get(Integer.class));

                final List<WrappedWatchableObject> wrappedWatchableObjectsNewMC = Arrays.asList(
                        // Invisibility itself
                        new WrappedWatchableObject(visibilityWatcher, visibleByte),
                        // Arrows in entity.
                        // IN 1.12.2 THIS IS AN INTEGER!
                        new WrappedWatchableObject(arrowInEntityWatcher, 0));
                entityMetadataWrapper.setMetadata(wrappedWatchableObjectsNewMC);
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
        entityMetadataWrapper.sendPacket(this.observedPlayer);

        this.visible = visible;
    }

    private void fakeAnimation(final int animationType)
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

    /**
     * Prevents bypasses based on the EntityID, especially for higher numbers
     *
     * @return the next free EntityID
     */
    private static int getNextEntityID() throws IllegalAccessException
    {
        // Get entity id for next entity (this one)
        final int entityID = entityCountField.getInt(null);

        // Increase entity id for next entity
        entityCountField.setInt(null, entityID + 1);
        return entityID;
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

        // Cancel hurt task
        if (hurtTask != null)
        {
            hurtTask.cancel();
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

package de.photon.AACAdditionPro.util.entities;

import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedWatchableObject;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.api.killauraentity.MovementType;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.entities.movement.Collision;
import de.photon.AACAdditionPro.util.entities.movement.Gravitation;
import de.photon.AACAdditionPro.util.entities.movement.Jumping;
import de.photon.AACAdditionPro.util.entities.movement.Movement;
import de.photon.AACAdditionPro.util.entities.movement.submovements.StayMovement;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class ClientsideEntity
{
    private static Field entityCountField;

    static
    {
        entityCountField = Reflect.fromNMS("Entity").field("entityCount").getField();
        entityCountField.setAccessible(true);
    }

    @Getter
    protected final int entityID;

    @Getter
    @Setter
    private boolean sprinting;

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

    @Getter
    protected final Player observedPlayer;

    @Getter
    protected final Hitbox hitbox;

    @Getter
    private boolean visible = true;

    private int tickTask = -1;

    // Movement state machine
    private Set<Movement> movementStates = new HashSet<>();
    private Movement currentMovementCalculator;

    public ClientsideEntity(final Player observedPlayer, Hitbox hitbox)
    {
        this.observedPlayer = observedPlayer;
        this.hitbox = hitbox;

        // Add all movements to the list
        this.movementStates.add(new StayMovement());

        // Set default movement state
        this.setMovement(MovementType.STAY);

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

    /**
     * Constructs a {@link ClientsideEntity} with additional possible {@link Movement}s.
     *
     * @param possibleMovements the additional {@link Movement}s the entity should be capable of. The {@link StayMovement} is added by default and should not be provided in the
     */
    public ClientsideEntity(final Player observedPlayer, Hitbox hitbox, Movement... possibleMovements)
    {
        this(observedPlayer, hitbox);
        this.movementStates.addAll(Arrays.asList(possibleMovements));
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

        if (!User.isUserInvalid(user))
        {
            final ClientsidePlayerEntity clientSidePlayerEntity = user.getClientSideEntityData().clientSidePlayerEntity;
            if (clientSidePlayerEntity != null)
            {
                return clientSidePlayerEntity.getEntityID() == this.entityID;
            }
        }
        return false;
    }

    /**
     * Should be called every tick once, updates physics + sends movement packets
     */
    protected void tick()
    {
        // Calculate velocity
        this.velocity = Gravitation.applyGravitationAndAirResistance(this.velocity, Gravitation.PLAYER);

        final Vector tempJumpVelocity = velocity.clone();
        tempJumpVelocity.setX(Math.signum(tempJumpVelocity.getX()));
        tempJumpVelocity.setZ(Math.signum(tempJumpVelocity.getZ()));

        // Whether the entity should jump if horizontally collided
        if (this.currentMovementCalculator.jumpIfCollidedHorizontally() &&
            this.location.clone().add(tempJumpVelocity).getBlock().isEmpty())
        {
            this.jump();
        }

        // ------------------------------------------ Movement system -----------------------------------------------//
        // Get the next position and move
        Vector xzVelocity = this.currentMovementCalculator.calculate(this.location.clone());

        // Backup-Movement
        if (xzVelocity == null)
        {
            this.setMovement(MovementType.STAY);
            xzVelocity = this.currentMovementCalculator.calculate(this.location.clone());
        }

        if (this.currentMovementCalculator.isTPNeeded() || this.needsTeleport)
        {
            final Location spawnLocation = observedPlayer.getLocation().clone().add(this.getMovement().calculate(observedPlayer.getLocation()));
            this.location = BlockUtils.getClosestFreeSpaceYAxis(spawnLocation, this.getHitbox());
        }
        else
        {
            // Only set the x- and the z- axis (y should be handled by autojumping).
            this.velocity.setX(xzVelocity.getX()).setZ(xzVelocity.getZ());
        }

        // ------------------------------------------ Velocity system -----------------------------------------------//
        final Vector collidedVelocity = Collision.getNearestUncollidedLocation(this.observedPlayer, this.location, this.hitbox, this.velocity);
        this.location = this.location.add(collidedVelocity);

        // Already added the velocity to location and collided it
        // ClientCopy
        this.onGround = (collidedVelocity.getY() != this.velocity.getY()) &&
                        // Due to gravity a player always have a negative velocity if walking/running on the ground.
                        velocity.getY() <= 0 &&
                        // Make sure the entity only jumps on real blocks, not e.g. grass.
                        BlockUtils.isJumpMaterial(this.location.clone().add(0, -0.05, 0).getBlock().getType());

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

        final boolean onGround = isOnGround();

        // Teleport needed ?
        int teleportThreshold;
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                teleportThreshold = 4;
                break;
            case MC110:
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
            // EntityID
            teleportWrapper.setEntityID(this.entityID);
            // Position
            teleportWrapper.setX(this.location.getX());
            teleportWrapper.setY(this.location.getY());
            teleportWrapper.setZ(this.location.getZ());
            // Angle
            teleportWrapper.setYaw(this.location.getYaw());
            teleportWrapper.setPitch(this.location.getPitch());
            // OnGround
            teleportWrapper.setOnGround(onGround);
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

                movePacketWrapper.setOnGround(onGround);
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
                lookPacketWrapper.setOnGround(onGround);

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

    private boolean isOnGround()
    {
        return this.onGround;
    }

    public void jump()
    {
        if (this.isOnGround())
        {
            velocity.setY(Jumping.getJumpYMotion(null));

            if (sprinting)
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
            headRotationWrapper.setHeadYaw(MathUtils.getFixRotation(headYaw));

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
               location.clone();
    }

    public Vector getVelocity()
    {
        return velocity.clone();
    }

    public void setVelocity(Vector velocity)
    {
        this.velocity = velocity.clone();
    }

    /**
     * Sets the {@link Movement} of this entity by the {@link MovementType}.
     *
     * @throws IllegalArgumentException if the Entity is not supporting the requested {@link Movement}.
     */
    public void setMovement(MovementType movementType)
    {
        for (Movement movement : movementStates)
        {
            if (movement.getMovementType() == movementType)
            {
                this.currentMovementCalculator = movement;
                return;
            }
        }
        throw new IllegalArgumentException("The Entity does not support the MovementType " + movementType.name());
    }

    public Movement getMovement()
    {
        return this.currentMovementCalculator;
    }

    // -------------------------------------------------------------- Simulation ------------------------------------------------------------ //

    /**
     * Fake being hurt by the observedPlayer in the next sync server tick
     */
    public void hurtByObserved()
    {
        Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
            lastHurtMillis = System.currentTimeMillis();
            hurt();

            Location observedLoc = observedPlayer.getLocation();
            observedLoc.setPitch(0);

            //Calculate knockback strength
            int knockbackStrength = 0;
            if (observedPlayer.isSprinting())
            {
                knockbackStrength = 1;
            }

            final ItemStack itemInHand;

            switch (ServerVersion.getActiveServerVersion())
            {
                case MC188:
                    itemInHand = observedPlayer.getItemInHand();
                    break;
                case MC110:
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

            //Apply velocity
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
        fakeAnimation(0);
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
        entityMetadataWrapper.setEntityID(this.getEntityID());

        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                final List<WrappedWatchableObject> wrappedWatchableObjectsOldMC = Arrays.asList(
                        // Invisibility itself
                        new WrappedWatchableObject(0, (byte) (
                                visible ?
                                0 :
                                0x20)),
                        // Arrows in entity.
                        // IN 1.8.8 THIS IS A BYTE, NOT AN INTEGER!
                        new WrappedWatchableObject(10, (byte) 0));
                entityMetadataWrapper.setMetadata(wrappedWatchableObjectsOldMC);
                break;

            case MC110:
            case MC111:
            case MC112:
                final WrappedDataWatcher.WrappedDataWatcherObject visibilityWatcher = new WrappedDataWatcher.WrappedDataWatcherObject(0, WrappedDataWatcher.Registry.get(Byte.class));
                final WrappedDataWatcher.WrappedDataWatcherObject arrowInEntityWatcher = new WrappedDataWatcher.WrappedDataWatcherObject(10, WrappedDataWatcher.Registry.get(Integer.class));

                entityMetadataWrapper.setEntityID(this.getEntityID());
                final List<WrappedWatchableObject> wrappedWatchableObjectsNewMC = Arrays.asList(
                        // Invisibility itself
                        new WrappedWatchableObject(visibilityWatcher, (byte) (
                                visible ?
                                0 :
                                0x20)),
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
        int entityID = entityCountField.getInt(null);

        // Increase entity id for next entity
        entityCountField.setInt(null, entityID + 1);
        return entityID;
    }

    // --------------------------------------------------------------- Despawn -------------------------------------------------------------- //

    public void despawn()
    {
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

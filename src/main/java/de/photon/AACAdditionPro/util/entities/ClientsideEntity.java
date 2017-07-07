package de.photon.AACAdditionPro.util.entities;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.entities.movement.Gravitation;
import de.photon.AACAdditionPro.util.entities.movement.Jumping;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.multiversion.ReflectionUtils;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerAnimation;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntity;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityDestroy;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityHeadRotation;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityLook;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityTeleport;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerRelEntityMove;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerRelEntityMoveLook;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;

public abstract class ClientsideEntity
{

    private static final Vector GRAVITY_VECTOR = Gravitation.PLAYER.getGravitationalVector();
    private static Field entityCountField;

    static {
        try {
            final String version = ReflectionUtils.getVersionString();
            Class<?> entityClass = Class.forName("net.minecraft.server." + version + ".Entity");
            entityCountField = entityClass.getDeclaredField("entityCount");
            entityCountField.setAccessible(true);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Server version is not supported", ex);
        }
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
    private boolean needsTeleport;

    /**
     * Stores the last timestamp this {@link ClientsideEntity} was hit.
     */
    private long lastHurtMillis;

    /**
     * The current velocity of this {@link ClientsideEntity}.
     */
    private Vector velocity = new Vector(0, 0, 0);

    protected Location lastLocation;
    protected Location location;

    @Getter
    protected float lastHeadYaw;
    @Getter
    protected float headYaw;

    @Getter
    protected final Player observedPlayer;

    public ClientsideEntity(final Player observedPlayer)
    {
        this.observedPlayer = observedPlayer;

        // Get a valid entity ID
        try {

            this.entityID = getNextEntityID();

        } catch (IllegalAccessException ex) {
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

        return (user != null) &&
               (user.getClientSideEntityData().clientSidePlayerEntity.getEntityID() == this.entityID);
    }

    // -------------------------------------------------------------- Simulation ------------------------------------------------------------ //

    /**
     * Should be called every tick once, updates physics + sends movement packets
     */
    public void tick()
    {
        //TODO ground calculations, pressing button simulation, etc?
        location.add(velocity);

        sendMove();
        sendHeadYaw();

        velocity.subtract(GRAVITY_VECTOR).multiply(.98);
    }

    /**
     * Moves the {@link ClientsideEntity} somewhere
     *
     * @param location the target {@link Location} this {@link ClientsideEntity} should be moved to.
     */
    public void move(Location location)
    {
        this.location = location;
    }

    /**
     * sends a fitting movement to from the last location to the current location
     */
    private void sendMove()
    {
        if (!this.spawned) {
            return;
        }
        double xDiff = this.location.getX() - this.lastLocation.getX();
        double yDiff = this.location.getY() - this.lastLocation.getY();
        double zDiff = this.location.getZ() - this.lastLocation.getZ();

        final boolean onGround = isOnGround();

        // Teleport needed ?
        int teleportThreshold;
        switch (AACAdditionPro.getInstance().getServerVersion()) {
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

        if (xDiff > teleportThreshold || yDiff > teleportThreshold || zDiff > teleportThreshold || needsTeleport) {
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
            System.out.println("Sent TP to: " + this.location.getX() + " | " + this.location.getY() + " | " + this.location.getZ());
        } else {
            // Sending relative movement
            boolean move = xDiff == 0 && yDiff == 0 && zDiff == 0;
            boolean look = this.location.getPitch() == this.lastLocation.getPitch() && this.location.getYaw() == this.lastLocation.getYaw();

            WrapperPlayServerEntity packetWrapper;

            if (move) {
                WrapperPlayServerRelEntityMove movePacketWrapper;

                if (look) {
                    WrapperPlayServerRelEntityMoveLook moveLookPacketWrapper = new WrapperPlayServerRelEntityMoveLook();

                    // Angle
                    moveLookPacketWrapper.setYaw(this.location.getYaw());
                    moveLookPacketWrapper.setPitch(this.location.getPitch());

                    movePacketWrapper = moveLookPacketWrapper;
                    System.out.println("Sending movelook");
                } else {
                    movePacketWrapper = new WrapperPlayServerRelEntityMove();
                    System.out.println("Sending move");
                }

                movePacketWrapper.setOnGround(onGround);
                movePacketWrapper.setDiffs(xDiff, yDiff, zDiff);
                packetWrapper = movePacketWrapper;

            } else if (look) {
                WrapperPlayServerEntityLook lookPacketWrapper = new WrapperPlayServerEntityLook();

                // Angles
                lookPacketWrapper.setYaw(this.location.getYaw());
                lookPacketWrapper.setPitch(this.location.getPitch());
                // OnGround
                lookPacketWrapper.setOnGround(onGround);

                packetWrapper = lookPacketWrapper;
                System.out.println("Sending look");

            } else {
                packetWrapper = new WrapperPlayServerEntity();
                System.out.println("Sending idle");
            }

            packetWrapper.setEntityID(this.entityID);
            packetWrapper.sendPacket(this.observedPlayer);
        }

        this.lastLocation = this.location.clone();
    }

    private boolean isOnGround()
    {
        return location.clone().add(0, -0.1, 0).getBlock().getType() != Material.AIR;
    }

    public void jump()
    {
        velocity.setY(Jumping.getJumpYMotion(Short.MIN_VALUE));

        if (sprinting) {
            velocity.add(location.getDirection().setY(0).normalize().multiply(.2F));
        }
    }

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
            if (observedPlayer.isSprinting()) {
                knockbackStrength = 1;
            }

            switch (AACAdditionPro.getInstance().getServerVersion()) {

                case MC188:
                    break;
                case MC110:
                    break;
                case MC111:
                    break;
                case MC112:
                    break;
                default:
                    throw new IllegalStateException("Unknown minecraft version");
            }
            //ItemStack itemInHand = observedPlayer.getItemInHand();
            ItemStack itemInHand = observedPlayer.getInventory().getItemInMainHand();

            if (itemInHand != null) {
                knockbackStrength += itemInHand.getEnchantmentLevel(Enchantment.KNOCKBACK);
            }

            //Apply velocity
            if (knockbackStrength > 0) {
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

    private void fakeAnimation(final int animationType)
    {
        // Entity is already spawned.
        if (this.isSpawned()) {
            final WrapperPlayServerAnimation animationWrapper = new WrapperPlayServerAnimation();
            animationWrapper.setEntityID(this.entityID);
            animationWrapper.setAnimation(animationType);
            animationWrapper.sendPacket(this.observedPlayer);
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

    // ---------------------------------------------------------------- Spawn --------------------------------------------------------------- //

    public abstract void spawn(Location location);

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
        final WrapperPlayServerEntityDestroy entityDestroyWrapper = new WrapperPlayServerEntityDestroy();
        entityDestroyWrapper.setEntityIds(new int[]{this.entityID});
        entityDestroyWrapper.sendPacket(observedPlayer);
        this.spawned = false;
    }
}

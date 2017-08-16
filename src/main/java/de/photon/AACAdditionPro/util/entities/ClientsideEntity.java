package de.photon.AACAdditionPro.util.entities;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.entities.movement.Gravitation;
import de.photon.AACAdditionPro.util.entities.movement.Jumping;
import de.photon.AACAdditionPro.util.mathematics.AxisAlignedBB;
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
import java.util.List;

public abstract class ClientsideEntity
{
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
    protected Vector size = new Vector();

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
    public long lastHurtMillis;

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
    private int tickTask = -1;
    private int ticks = 0;

    public ClientsideEntity(final Player observedPlayer)
    {
        this.observedPlayer = observedPlayer;

        tickTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), this::tick, 1L, 1L);

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

        if (user != null) {
            ClientsidePlayerEntity clientSidePlayerEntity = user.getClientSideEntityData().clientSidePlayerEntity;
            if (clientSidePlayerEntity != null) {
                if (clientSidePlayerEntity.getEntityID() == this.entityID) {
                    return true;
                }
            }
        }
        return false;
    }

    // -------------------------------------------------------------- Simulation ------------------------------------------------------------ //

    /**
     * Should be called every tick once, updates physics + sends movement packets
     */
    protected void tick()
    {
        // Apply motion movement
        velocity = Gravitation.applyDrag(velocity.add(Gravitation.PLAYER.getGravitationalVector()));

        double dX = velocity.getX();
        double dY = velocity.getY();
        double dZ = velocity.getZ();

        // Since we need collision detections now we need the NMS world
        AxisAlignedBB bb = new AxisAlignedBB(
                this.location.getX() - (this.size.getX() / 2),
                // The location is based on the feet location
                this.location.getY(),
                this.location.getZ() - (this.size.getZ() / 2),

                this.location.getX() + (this.size.getX() / 2),
                // The location is based on the feet location
                this.location.getY() + this.size.getY(),
                this.location.getZ() + (this.size.getZ() / 2)
        );
        List<AxisAlignedBB> collisions = ReflectionUtils.getCollisionBoxes(observedPlayer, bb.addCoordinates(dX, dY, dZ));

        // Check if we would hit a y border block
        for (AxisAlignedBB axisAlignedBB : collisions) {
            dY = axisAlignedBB.calculateYOffset(bb, dY);
        }

        bb.offset(0, dY, 0);

        // Check if we would hit a x border block
        for (AxisAlignedBB axisAlignedBB : collisions) {
            dX = axisAlignedBB.calculateXOffset(bb, dX);
        }

        bb.offset(dX, 0, 0);

        // Check if we would hit a z border block
        for (AxisAlignedBB axisAlignedBB : collisions) {
            dZ = axisAlignedBB.calculateZOffset(bb, dZ);
        }

        bb.offset(0, 0, dZ);

        // Move
        location.add(dX, dY, dZ);

        sendMove();
        sendHeadYaw();
    }

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

        if (Math.abs(xDiff) + Math.abs(yDiff) + Math.abs(zDiff) > teleportThreshold || needsTeleport) {
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
        } else {
            // Sending relative movement
            boolean move = xDiff != 0 || yDiff != 0 || zDiff != 0;
            boolean look = this.location.getPitch() != this.lastLocation.getPitch() || this.location.getYaw() != this.lastLocation.getYaw();

            WrapperPlayServerEntity packetWrapper;

            if (move) {
                WrapperPlayServerRelEntityMove movePacketWrapper;

                if (look) {
                    WrapperPlayServerRelEntityMoveLook moveLookPacketWrapper = new WrapperPlayServerRelEntityMoveLook();

                    // Angle
                    moveLookPacketWrapper.setYaw(this.location.getYaw());
                    moveLookPacketWrapper.setPitch(this.location.getPitch());

                    movePacketWrapper = moveLookPacketWrapper;
                    // System.out.println("Sending movelook");
                } else {
                    movePacketWrapper = new WrapperPlayServerRelEntityMove();
                    // System.out.println("Sending move");
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
                // System.out.println("Sending look");

            } else {
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

            final ItemStack itemInHand;

            switch (AACAdditionPro.getInstance().getServerVersion()) {
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
        if (tickTask > 0) {
            Bukkit.getScheduler().cancelTask(tickTask);
            this.tickTask = -1;
        }

        if (spawned) {
            final WrapperPlayServerEntityDestroy entityDestroyWrapper = new WrapperPlayServerEntityDestroy();
            entityDestroyWrapper.setEntityIds(new int[]{this.entityID});
            entityDestroyWrapper.sendPacket(observedPlayer);
            this.spawned = false;
        }
    }
}

package de.photon.AACAdditionPro.util.clientsideentities;

import com.comphenix.protocol.wrappers.EnumWrappers;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.multiversion.ReflectionUtils;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerAnimation;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntity;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityDestroy;
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

    private static final Vector GRAVITY_VECTOR = new Vector(0, -.08, 0);
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
    private long lastHit;

    /**
     * The current velocity of this {@link ClientsideEntity}.
     */
    private Vector velocity = new Vector(0, 0, 0);

    @Getter
    protected Location location;

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

    /**
     * Moves the {@link ClientsideEntity} somewhere
     *
     * @param location the target {@link Location} this {@link ClientsideEntity} should be moved to.
     */
    public void move(final Location location)
    {
        if (!this.spawned) {
            return;
        }
        double xDiff = location.getX() - this.location.getX();
        double yDiff = location.getY() - this.location.getY();
        double zDiff = location.getZ() - this.location.getZ();

        final boolean onGround = location.clone().add(0, -0.1, 0).getBlock().getType() != Material.AIR;

        // Teleport needed ?
        int teleportThreshold;
        switch (AACAdditionPro.getInstance().getServerVersion()) {
            case MC188:
                teleportThreshold = 4;
                break;
            default:
                teleportThreshold = 8;
                break;
        }
        if (xDiff > teleportThreshold || yDiff > teleportThreshold || zDiff > teleportThreshold || needsTeleport) {
            final WrapperPlayServerEntityTeleport teleportWrapper = new WrapperPlayServerEntityTeleport();
            // EntityID
            teleportWrapper.setEntityID(this.entityID);
            // Position
            teleportWrapper.setX(location.getX());
            teleportWrapper.setY(location.getY());
            teleportWrapper.setZ(location.getZ());
            // Angle
            teleportWrapper.setYaw(location.getYaw());
            teleportWrapper.setPitch(location.getPitch());
            // OnGround
            teleportWrapper.setOnGround(onGround);
            // Send the packet
            teleportWrapper.sendPacket(this.observedPlayer);
            this.needsTeleport = false;
            System.out.println("Sent TP to: " + location.getX() + " | " + location.getY() + " | " + location.getZ());
        } else {
            //Sending relative movement
            boolean move = xDiff == 0 && yDiff == 0 && zDiff == 0;
            boolean look = location.getPitch() == this.location.getPitch() && location.getYaw() == this.location.getYaw();

            WrapperPlayServerEntity packetWrapper;

            if (move) {
                WrapperPlayServerRelEntityMove movePacketWrapper;

                if (look) {
                    WrapperPlayServerRelEntityMoveLook moveLookPacketWrapper = new WrapperPlayServerRelEntityMoveLook();

                    // Angle
                    moveLookPacketWrapper.setYaw(location.getYaw());
                    moveLookPacketWrapper.setPitch(location.getPitch());

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
                lookPacketWrapper.setYaw(location.getYaw());
                lookPacketWrapper.setPitch(location.getPitch());
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

        this.location = location.clone();
    }

    public void tick()
    {
        //TODO ground calculations, pressing button simulation, etc?
        location.add(velocity);

        velocity.subtract(GRAVITY_VECTOR).multiply(.98);
    }

    public void jump()
    {
        velocity.setY(.42);

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
            lastHit = System.currentTimeMillis();
            hurt();

            Location observedLoc = observedPlayer.getLocation();
            observedLoc.setPitch(0);
            
            //Calculate knockback strength
            int knockbackStrength = 0;
            if (observedPlayer.isSprinting()) {
                knockbackStrength = 1;
            }
            ItemStack itemInHand = observedPlayer.getItemInHand();
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

    public boolean isValid()
    {
        final User user = UserManager.getUser(observedPlayer.getUniqueId());

        return (user != null) &&
               (user.getClientSideEntityData().clientSidePlayerEntity.getEntityID() == this.entityID);
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
        if (this.spawned) {
            final WrapperPlayServerAnimation animationWrapper = new WrapperPlayServerAnimation();
            animationWrapper.setEntityID(this.entityID);
            animationWrapper.setAnimation(animationType);
            animationWrapper.sendPacket(this.observedPlayer);
        }
    }

    public abstract void spawn();

    public void despawn()
    {
        final WrapperPlayServerEntityDestroy entityDestroyWrapper = new WrapperPlayServerEntityDestroy();
        entityDestroyWrapper.setEntityIds(new int[]{this.entityID});
        entityDestroyWrapper.sendPacket(observedPlayer);
        this.spawned = false;
    }

    private static int getNextEntityID() throws IllegalAccessException
    {
        // Get entity id for next entity (this one)
        int entityID = entityCountField.getInt(null);

        // Increase entity id for next entity
        entityCountField.setInt(null, entityID + 1);
        return entityID;
    }
}

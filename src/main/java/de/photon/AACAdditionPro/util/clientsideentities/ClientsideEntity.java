package de.photon.AACAdditionPro.util.clientsideentities;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.multiversion.ReflectionUtils;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerAnimation;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityDestroy;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityLook;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityTeleport;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerRelEntityMove;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerRelEntityMoveLook;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;

public abstract class ClientsideEntity
{
    @Getter
    protected int entityID = 0;

    /**Determines whether this {@link ClientsideEntity} is already spawned.*/
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

    public Location currentLocation;

    @Getter
    protected final Player observedPlayer;

    public ClientsideEntity(final Player observedPlayer)
    {
        this.observedPlayer = observedPlayer;

        // Get a valid entity ID
        try {
            //Get the String representing the version, e.g. v1_11_R1
            final String version = ReflectionUtils.getVersionString();

            // Get the class
            final Class entityPlayerClazz = ReflectionUtils.loadClassFromPath("net.minecraft.server." + version + ".Entity");

            // Get the wanted field
            final Field entityCountField = entityPlayerClazz.getDeclaredField("entityCount");
            entityCountField.setAccessible(true);

            // Set the correct entityCount
            this.entityID = entityCountField.getInt(null);

            // Set the field value
            entityCountField.setInt(null, entityID + 1);

        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        if (entityID == 0) {
            throw new RuntimeException("Could not create ClientsideEntity for player " + observedPlayer.getName());
        }
    }

    /**
     * Moves the {@link ClientsideEntity} somewhere
     *
     * @param location the target {@link Location} this {@link ClientsideEntity} should be moved to.
     */
    private void move(final Location location)
    {
        if (this.spawned) {
            /*Depicts the different movement actions in decreasing priority, teleport is handled directly:
              [0] == movement
              [1] == rotation*/
            final boolean[] movement = new boolean[3];

            /*Stores the location differences:
              [0] == x-Difference
              [1] == y-Difference
              [2] == z-Difference*/
            final double[] loc_Diff = new double[3];
            loc_Diff[0] = location.getX() - this.currentLocation.getX();
            loc_Diff[1] = location.getY() - this.currentLocation.getY();
            loc_Diff[2] = location.getZ() - this.currentLocation.getZ();

            final boolean onGround = location.clone().add(0, -0.1, 0).getBlock().getType() != Material.AIR;

            // Teleport needed ?
            for (final double diff : loc_Diff) {
                if (diff > 8 || needsTeleport) {
                    final WrapperPlayServerEntityTeleport teleportWrapper = new WrapperPlayServerEntityTeleport();
                    // EntityID
                    teleportWrapper.setEntityID(this.entityID);
                    // Position
                    teleportWrapper.setX(location.getX());
                    teleportWrapper.setY(location.getY());
                    teleportWrapper.setZ(location.getZ());
                    // Angle
                    teleportWrapper.setYaw(MathUtils.getFixRotation(location.getYaw()));
                    teleportWrapper.setPitch(MathUtils.getFixRotation(location.getPitch()));
                    // OnGround
                    teleportWrapper.setOnGround(onGround);
                    // Send the packet
                    teleportWrapper.sendPacket(this.observedPlayer);
                    this.needsTeleport = false;
                    System.out.println("Sent TP to: " + location.getX() + " | " + location.getY() + " | " + location.getZ());
                    break;
                } else if (diff > 0) {
                    movement[0] = true;
                }
            }

            System.out.println("Movement");

            // Movement
            if (movement[0]) {
                final double[] move_loc_Diff = new double[3];

                for (byte b = 0; b < move_loc_Diff.length; b++) {
                    move_loc_Diff[b] = loc_Diff[b] /* * 32*/;
                }

                // Movement + Rotation
                if (movement[1]) {
                    final WrapperPlayServerRelEntityMoveLook relEntityMoveLookWrapper = new WrapperPlayServerRelEntityMoveLook();
                    // EntityID
                    relEntityMoveLookWrapper.setEntityID(this.entityID);
                    // Relative movement
                    relEntityMoveLookWrapper.setDx(move_loc_Diff[0]);
                    relEntityMoveLookWrapper.setDy(move_loc_Diff[1]);
                    relEntityMoveLookWrapper.setDz(move_loc_Diff[2]);
                    // Angle
                    relEntityMoveLookWrapper.setYaw(MathUtils.getFixRotation(location.getYaw()));
                    relEntityMoveLookWrapper.setPitch(MathUtils.getFixRotation(location.getPitch()));
                    // OnGround
                    relEntityMoveLookWrapper.setOnGround(onGround);
                    // Send packet
                    relEntityMoveLookWrapper.sendPacket(this.observedPlayer);
                    System.out.println("Sent RotMove");
                    // Only Movement
                } else {
                    final WrapperPlayServerRelEntityMove relEntityMoveWrapper = new WrapperPlayServerRelEntityMove();
                    // EntityID
                    relEntityMoveWrapper.setEntityID(this.entityID);
                    // Relative movement
                    relEntityMoveWrapper.setDx((int) move_loc_Diff[0]);
                    relEntityMoveWrapper.setDy((int) move_loc_Diff[1]);
                    relEntityMoveWrapper.setDz((int) move_loc_Diff[2]);
                    // OnGround
                    relEntityMoveWrapper.setOnGround(onGround);
                    // Send packet
                    relEntityMoveWrapper.sendPacket(this.observedPlayer);
                    System.out.println("Sent Move");
                }

                // Only Rotation
            } else if (movement[1]) {
                final WrapperPlayServerEntityLook entityLookWrapper = new WrapperPlayServerEntityLook();
                // EntityID
                entityLookWrapper.setEntityID(this.entityID);
                // Angles
                entityLookWrapper.setYaw(MathUtils.getFixRotation(location.getYaw()));
                entityLookWrapper.setPitch(MathUtils.getFixRotation(location.getPitch()));
                // OnGround
                entityLookWrapper.setOnGround(onGround);
                // Send packet
                entityLookWrapper.sendPacket(this.observedPlayer);
                System.out.println("Sent Rot");
            }

            this.currentLocation = location;
        }
    }

    public void fakeHit()
    {
        Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), () -> {
            lastHit = System.currentTimeMillis();
            hurt();

            // Fake knockback
            double motX = (-Math.sin(observedPlayer.getLocation().getYaw() * Math.PI / 180.0F) * 0.5F);
            final double motY = 0.1D;
            double motZ = (Math.cos(observedPlayer.getLocation().getYaw() * Math.PI / 180.0F) * 0.5F);

            motX *= 0.6D;
            motZ *= 0.6D;

            velocity = new Vector(motX, motY, motZ);
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
    protected void swing()
    {
        fakeAnimation(0);
    }

    /**
     * Fakes the hurt - animation to make the entity look like it was hurt
     */
    protected void hurt()
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

    protected abstract void spawn();

    protected void despawn()
    {
        final WrapperPlayServerEntityDestroy entityDestroyWrapper = new WrapperPlayServerEntityDestroy();
        entityDestroyWrapper.setEntityIds(new int[]{this.entityID});
        entityDestroyWrapper.sendPacket(observedPlayer);
        this.spawned = false;
        postDespawn();
    }

    protected abstract void postDespawn();
}

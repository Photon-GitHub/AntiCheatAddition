package de.photon.AACAdditionPro.util.fakeentity;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.util.fakeentity.displayinformation.DisplayInformation;
import de.photon.AACAdditionPro.util.fakeentity.equipment.Equipment;
import de.photon.AACAdditionPro.util.fakeentity.movement.submovements.BasicFollowMovement;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.mathematics.RotationUtil;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerEntityEquipment;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerNamedEntitySpawn;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.concurrent.ThreadLocalRandom;

public class ClientsidePlayerEntity extends ClientsideEntity
{
    private final boolean visible_in_tablist;
    private final boolean shouldAssignTeam;
    private final boolean shouldSwing;
    private boolean shouldSwap;
    private final boolean onlineProfile;

    @Getter
    private final WrappedGameProfile gameProfile;

    // Ping handling
    private int ping;
    private int pingTask;

    @Getter
    private Team currentTeam;

    // Main ticker for the entity
    private short lastJump = 0;
    private short lastSwing = 0;
    private short lastHandSwap = 0;
    private short lastArmorSwap = 0;

    private final Equipment equipment;

    public ClientsidePlayerEntity(final Player observedPlayer, final WrappedGameProfile gameProfile, boolean onlineProfile)
    {
        super(observedPlayer, Hitbox.PLAYER, new BasicFollowMovement());

        // Get skin data and name
        this.gameProfile = gameProfile;
        this.onlineProfile = onlineProfile;

        // EquipmentData
        this.equipment = new Equipment(this, ServerVersion.getClientServerVersion(observedPlayer) != ServerVersion.MC188);

        // Init additional behaviour configs
        visible_in_tablist = AACAdditionPro.getInstance().getConfig().getBoolean(ModuleType.KILLAURA_ENTITY.getConfigString() + ".behaviour.visible_in_tablist") &&
                             // Online profiles do not need scoreboard manipulations.
                             !AACAdditionPro.getInstance().getConfig().getBoolean(ModuleType.KILLAURA_ENTITY.getConfigString() + ".prefer_online_profiles");
        shouldAssignTeam = AACAdditionPro.getInstance().getConfig().getBoolean(ModuleType.KILLAURA_ENTITY.getConfigString() + ".behaviour.team.enabled");
        shouldSwing = AACAdditionPro.getInstance().getConfig().getBoolean(ModuleType.KILLAURA_ENTITY.getConfigString() + ".behaviour.swing.enabled");
        shouldSwap = AACAdditionPro.getInstance().getConfig().getBoolean(ModuleType.KILLAURA_ENTITY.getConfigString() + ".behaviour.swap.enabled");
    }

    @Override
    protected void tick()
    {
        super.tick();

        // Teams + Scoreboard
        if (shouldAssignTeam)
        {
            DisplayInformation.applyTeams(this);
        }

        // Try to look to the target
        Location target = this.observedPlayer.getLocation();
        double diffX = target.getX() - this.location.getX();
        double diffY = target.getY() + this.observedPlayer.getEyeHeight() * 0.9D - (this.location.getY() + this.observedPlayer.getEyeHeight());
        double diffZ = target.getZ() - this.location.getZ();
        double dist = Math.hypot(diffX, diffZ);

        // Yaw
        float yaw = (float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0F;
        this.headYaw = RotationUtil.wrapToAllowedYaw((float) MathUtils.randomBoundaryDouble(yaw - 10, 20));
        this.location.setYaw(yaw);

        // Pitch
        float pitch = (float) Math.toDegrees(Math.asin(diffY / dist));
        pitch += ThreadLocalRandom.current().nextInt(5);
        pitch = RotationUtil.reduceAngle(pitch, 90);
        this.location.setPitch(pitch);

        this.move(this.location);

        // Maybe we should switch movement states?
        if (lastJump++ > MathUtils.randomBoundaryInt(30, 80))
        {
            lastJump = 0;
            jump();
        }

        // Swap items if needed
        if (shouldSwap)
        {
            if (lastHandSwap++ > MathUtils.randomBoundaryInt(40, 65))
            {
                lastHandSwap = 0;
                // Automatic offhand handling
                equipment.replaceHands();
                // Send the updated Equipment
                equipment.updateEquipment();
            }

            if (lastArmorSwap++ > MathUtils.randomBoundaryInt(200, 200))
            {
                lastArmorSwap = 0;
                // Automatic offhand handling
                equipment.replaceRandomArmorPiece();
                // Send the updated Equipment
                equipment.updateEquipment();
            }
        }

        // Swing items if enabled
        if (shouldSwing)
        {
            if (lastSwing++ > MathUtils.randomBoundaryInt(15, 55))
            {
                lastSwing = 0;
                swing();
            }
        }
    }

    // --------------------------------------------------------------- General -------------------------------------------------------------- //

    /**
     * Gets the name of the entity from its game profile.
     */
    public String getName()
    {
        return this.gameProfile.getName();
    }

    // -------------------------------------------------------------- Simulation ------------------------------------------------------------ //

    /**
     * This changes the Ping of the {@link ClientsidePlayerEntity}.
     * The recursive call is needed to randomize the ping-update
     */
    private void recursiveUpdatePing()
    {
        pingTask = Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> {
            if (this.isValid())
            {
                // Fake the ping if the entity is already spawned
                if (this.isSpawned())
                {
                    this.ping = MathUtils.randomBoundaryInt(21, 4);
                    this.updatePlayerInfo(EnumWrappers.PlayerInfoAction.UPDATE_LATENCY, this.ping);
                }

                recursiveUpdatePing();
            }
        }, (long) MathUtils.randomBoundaryInt(15, 40));
    }

    @Override
    public void setVisibility(final boolean visible)
    {
        super.setVisibility(visible);

        this.shouldSwap = visible;

        if (!visible)
        {
            WrapperPlayServerEntityEquipment.clearAllSlots(this.entityID, this.observedPlayer);
        }
    }

    // ---------------------------------------------------------------- Teams --------------------------------------------------------------- //

    /**
     * Used to make the {@link ClientsidePlayerEntity} join a new {@link Team}
     * If the {@link ClientsidePlayerEntity} is already in a {@link Team} it will try to leave it first.
     *
     * @param team the new {@link Team} to join.
     */
    public void joinTeam(Team team) throws IllegalStateException
    {
        this.leaveTeam();
        team.addEntry(this.gameProfile.getName());
        this.currentTeam = team;
    }

    /**
     * Used to make the {@link ClientsidePlayerEntity} leave its current {@link Team}
     * If the {@link ClientsidePlayerEntity} is in no team nothing will happen.
     */
    private void leaveTeam() throws IllegalStateException
    {
        if (this.currentTeam != null)
        {
            this.currentTeam.removeEntry(this.gameProfile.getName());
            this.currentTeam = null;
        }
    }

    // ---------------------------------------------------------------- Spawn --------------------------------------------------------------- //

    @Override
    public void spawn(Location location)
    {
        super.spawn(location);
        this.lastLocation = location.clone();
        this.move(location);

        // Add the player in the Tablist via PlayerInfo
        this.updatePlayerInfo(EnumWrappers.PlayerInfoAction.ADD_PLAYER, this.ping);

        // DataWatcher
        final WrappedDataWatcher dataWatcher = new WrappedDataWatcher();

        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                dataWatcher.setObject(6, 20F);
                dataWatcher.setObject(10, (byte) 127);
                break;
            case MC110:
            case MC111:
            case MC112:
                WrappedDataWatcher.WrappedDataWatcherObject[] objects = new WrappedDataWatcher.WrappedDataWatcherObject[2];
                objects[0] = new WrappedDataWatcher.WrappedDataWatcherObject(6, WrappedDataWatcher.Registry.get(Byte.class));
                objects[1] = new WrappedDataWatcher.WrappedDataWatcherObject(10, WrappedDataWatcher.Registry.get(Byte.class));
                dataWatcher.setObject(objects[0], (byte) 20);
                dataWatcher.setObject(objects[1], (byte) 127);
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }

        // Spawn the entity
        final WrapperPlayServerNamedEntitySpawn spawnEntityWrapper = new WrapperPlayServerNamedEntitySpawn();

        spawnEntityWrapper.setEntityID(this.entityID);
        spawnEntityWrapper.setMetadata(dataWatcher);
        spawnEntityWrapper.setPosition(location.toVector());
        spawnEntityWrapper.setPlayerUUID(this.gameProfile.getUUID());
        spawnEntityWrapper.setYaw(ThreadLocalRandom.current().nextInt(15));
        spawnEntityWrapper.setPitch(ThreadLocalRandom.current().nextInt(15));

        spawnEntityWrapper.sendPacket(observedPlayer);

        // Debug
        // System.out.println("Sent player spawn of bot " + this.entityID + " for " + observedPlayer.getName() + " @ " + location);

        // Set the team (most common on respawn)
        if (shouldAssignTeam)
        {
            DisplayInformation.applyTeams(this);
        }

        if (this.visible_in_tablist)
        {
            this.recursiveUpdatePing();
        }
        else if (!this.onlineProfile)
        {
            this.updatePlayerInfo(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, this.ping);
        }

        // Entity equipment + armor
        this.equipment.replaceArmor();
        // Automatic offhand handling
        this.equipment.replaceHands();
        this.equipment.updateEquipment();
    }

    // --------------------------------------------------------------- Despawn -------------------------------------------------------------- //

    @Override
    public void despawn()
    {
        if (isSpawned())
        {
            if (this.visible_in_tablist && !this.onlineProfile)
            {
                this.updatePlayerInfo(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER, 0);
            }
            Bukkit.getScheduler().cancelTask(pingTask);
        }

        super.despawn();
    }

    /**
     * Updates the PlayerInformation of a player (or {@link ClientsidePlayerEntity}).
     * This can be used to update the ping ({@link com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction#UPDATE_LATENCY}) <br>
     * or to add ({@link com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction#ADD_PLAYER}) and
     * remove ({@link com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction#REMOVE_PLAYER}) a player from the tablist
     */
    private void updatePlayerInfo(EnumWrappers.PlayerInfoAction action, int ping)
    {
        DisplayInformation.updatePlayerInformation(action,
                                                   this.gameProfile,
                                                   ping,
                                                   EnumWrappers.NativeGameMode.SURVIVAL,
                                                   null,
                                                   this.observedPlayer);
    }
}

package de.photon.AACAdditionPro.util.clientsideentities;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.util.clientsideentities.displayinformation.DisplayInformation;
import de.photon.AACAdditionPro.util.clientsideentities.equipment.EntityEquipmentUtils;
import de.photon.AACAdditionPro.util.clientsideentities.equipment.EquipmentType;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerNamedEntitySpawn;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPlayerInfo;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class ClientsidePlayerEntity extends ClientsideEntity
{
    @Getter
    private final WrappedGameProfile gameProfile;
    @Getter
    @Setter
    private int ping;

    public Team currentTeam;

    private int[] tasks = new int[2];

    public ClientsidePlayerEntity(final Player observedPlayer, final WrappedGameProfile gameProfile)
    {
        super(observedPlayer);
        // Get skin data and name
        this.gameProfile = gameProfile;

        tasks[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), () ->
        {
            // TODO: INVOKE checkRespawn() and checkScoreboard() every tick!
        }, 0L, 1L);

        DisplayInformation.updatePing(this);
    }

    // --------------------------------------------------------------- General -------------------------------------------------------------- //

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
        Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> {
            DisplayInformation.updatePing(this);
            recursiveUpdatePing();
        }, 10 + ThreadLocalRandom.current().nextInt(35));
    }

    // ---------------------------------------------------------------- Spawn --------------------------------------------------------------- //

    @Override
    public void spawn()
    {
        // Add the player with PlayerInfo
        final PlayerInfoData playerInfoData = new PlayerInfoData(this.gameProfile, ping, EnumWrappers.NativeGameMode.SURVIVAL, null);

        final WrapperPlayServerPlayerInfo playerInfoWrapper = new WrapperPlayServerPlayerInfo();
        playerInfoWrapper.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        playerInfoWrapper.setData(Collections.singletonList(playerInfoData));

        playerInfoWrapper.sendPacket(observedPlayer);

        // Debug
        System.out.println("Spawned bot " + this.entityID + " for " + observedPlayer.getName() + " @ " + location);

        // DataWatcher
        final WrappedDataWatcher dataWatcher = new WrappedDataWatcher();
        dataWatcher.setObject(6, (float) 20);
        dataWatcher.setObject(10, (byte) 127); //TODO player's probably do have more things set here

        // Spawn the entity
        final WrapperPlayServerNamedEntitySpawn spawnEntityWrapper = new WrapperPlayServerNamedEntitySpawn();

        spawnEntityWrapper.setEntityID(this.entityID);
        spawnEntityWrapper.setMetadata(dataWatcher);
        spawnEntityWrapper.setPosition(location.toVector());
        spawnEntityWrapper.setPlayerUUID(this.gameProfile.getUUID());
        spawnEntityWrapper.setYaw(ThreadLocalRandom.current().nextInt(15));
        spawnEntityWrapper.setPitch(ThreadLocalRandom.current().nextInt(15));

        spawnEntityWrapper.sendPacket(observedPlayer);

        // Set the team (most common on respawn)
        DisplayInformation.applyTeams(this);

        // Entity equipment + armor
        EntityEquipmentUtils.equipPlayerEntity(observedPlayer, this, EquipmentType.ARMOR);
        EntityEquipmentUtils.equipPlayerEntity(observedPlayer, this, EquipmentType.NORMAL);
    }


    // --------------------------------------------------------------- Despawn -------------------------------------------------------------- //

    private void removeFromTab()
    {
        // Remove the player with PlayerInfo
        final WrappedGameProfile gameProfile = WrappedGameProfile.fromHandle(this.gameProfile);
        final PlayerInfoData playerInfoData = new PlayerInfoData(gameProfile, 0, EnumWrappers.NativeGameMode.SURVIVAL, null);

        final WrapperPlayServerPlayerInfo playerInfoWrapper = new WrapperPlayServerPlayerInfo();
        playerInfoWrapper.setAction(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
        playerInfoWrapper.setData(Collections.singletonList(playerInfoData));

        playerInfoWrapper.sendPacket(observedPlayer);
    }

    @Override
    public void despawn()
    {
        // Cancel all tasks of this entity
        for (int i : tasks) {
            Bukkit.getScheduler().cancelTask(i);
        }
        super.despawn();
        removeFromTab();
    }
}

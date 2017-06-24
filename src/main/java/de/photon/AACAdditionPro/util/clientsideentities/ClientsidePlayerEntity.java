package de.photon.AACAdditionPro.util.clientsideentities;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.AACAdditionPro.util.clientsideentities.equipment.EntityEquipmentUtils;
import de.photon.AACAdditionPro.util.clientsideentities.equipment.EquipmentType;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerNamedEntitySpawn;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPlayerInfo;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

public class ClientsidePlayerEntity extends ClientsideEntity
{
    @Getter
    private final WrappedGameProfile gameProfile;

    public Team currentTeam;

    public ClientsidePlayerEntity(final Player observedPlayer, final WrappedGameProfile gameProfile)
    {
        super(observedPlayer);
        // Get skin data and name
        this.gameProfile = gameProfile;

        // TODO: INVOKE checkRespawn() and checkScoreboard() every tick!
        // TODO: UPDATE THE PING REGULARY (DISPLAYINFORMATION HAS A METHOD FOR THIS)
    }

    public String getName()
    {
        return this.gameProfile.getName();
    }

    @Override
    protected void spawn()
    {
        // Add the player with PlayerInfo
        final WrappedGameProfile gameProfile = WrappedGameProfile.fromHandle(this.gameProfile);
        final PlayerInfoData playerInfoData = new PlayerInfoData(gameProfile, 0, EnumWrappers.NativeGameMode.SURVIVAL, null);

        final WrapperPlayServerPlayerInfo playerInfoWrapper = new WrapperPlayServerPlayerInfo();
        playerInfoWrapper.setAction(EnumWrappers.PlayerInfoAction.ADD_PLAYER);
        playerInfoWrapper.setData(Collections.singletonList(playerInfoData));

        playerInfoWrapper.sendPacket(observedPlayer);

        // Debug
        System.out.println("Spawned bot " + this.entityID + " for " + observedPlayer.getName() + " @ " + currentLocation);

        // DataWatcher
        final WrappedDataWatcher dataWatcher = new WrappedDataWatcher();
        dataWatcher.setObject(6, (float) 20);
        dataWatcher.setObject(10, (byte) 127);

        // Spawn the entity
        final WrapperPlayServerNamedEntitySpawn spawnEntityWrapper = new WrapperPlayServerNamedEntitySpawn();

        spawnEntityWrapper.setEntityID(this.entityID);
        spawnEntityWrapper.setMetadata(dataWatcher);
        spawnEntityWrapper.setPosition(currentLocation.toVector());
        spawnEntityWrapper.setPlayerUUID(gameProfile.getUUID());
        spawnEntityWrapper.setYaw(ThreadLocalRandom.current().nextInt(15));
        spawnEntityWrapper.setPitch(ThreadLocalRandom.current().nextInt(15));

        spawnEntityWrapper.sendPacket(observedPlayer);

        // Entity equipment + armor
        EntityEquipmentUtils.equipPlayerEntity(observedPlayer, this, EquipmentType.ARMOR);
        EntityEquipmentUtils.equipPlayerEntity(observedPlayer, this, EquipmentType.NORMAL);
    }

    @Override
    protected void postDespawn()
    {
        // Remove the player with PlayerInfo
        final WrappedGameProfile gameProfile = WrappedGameProfile.fromHandle(this.gameProfile);
        final PlayerInfoData playerInfoData = new PlayerInfoData(gameProfile, 0, EnumWrappers.NativeGameMode.SURVIVAL, null);

        final WrapperPlayServerPlayerInfo playerInfoWrapper = new WrapperPlayServerPlayerInfo();
        playerInfoWrapper.setAction(EnumWrappers.PlayerInfoAction.REMOVE_PLAYER);
        playerInfoWrapper.setData(Collections.singletonList(playerInfoData));

        playerInfoWrapper.sendPacket(observedPlayer);
    }
}

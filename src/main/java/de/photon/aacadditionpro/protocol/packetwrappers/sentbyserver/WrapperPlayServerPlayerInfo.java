package de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.aacadditionpro.protocol.packetwrappers.AbstractPacket;
import lombok.val;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class WrapperPlayServerPlayerInfo extends AbstractPacket
{
    public static final PacketType TYPE = PacketType.Play.Server.PLAYER_INFO;

    public WrapperPlayServerPlayerInfo()
    {
        super(new PacketContainer(TYPE), TYPE);
        handle.getModifier().writeDefaults();
    }

    public WrapperPlayServerPlayerInfo(PacketContainer packet)
    {
        super(packet, TYPE);
    }

    /**
     * This method updates the player information, and thus the tablist of a player.
     *
     * @param action         the {@link PlayerInfoAction} that will be executed.
     * @param gameProfile    the {@link WrappedGameProfile} of the player whose information will be updated.<br>
     *                       Use {@link WrappedGameProfile#fromPlayer(Player)} in order to get a {@link WrappedGameProfile} from a {@link Player}.
     * @param ping           the new ping of the updated {@link Player}.
     *                       Use {@link de.photon.aacadditionpro.util.minecraft.ping.PingProvider#getPing(Player)} to get the ping of a {@link Player}.
     * @param gameMode       the {@link EnumWrappers.NativeGameMode} of the updated {@link Player}.
     * @param displayName    the new displayName of the updated {@link Player}
     * @param affectedPlayer the {@link Player} who will see the updated information as the packet is sent to him.
     */
    public static void updatePlayerInformation(final PlayerInfoAction action, final WrappedGameProfile gameProfile, final int ping, final EnumWrappers.NativeGameMode gameMode, final WrappedChatComponent displayName, final Player affectedPlayer)
    {
        val playerInfoData = new PlayerInfoData(gameProfile, ping, gameMode, displayName);

        val playerInfoWrapper = new WrapperPlayServerPlayerInfo();
        playerInfoWrapper.setAction(action);
        playerInfoWrapper.setData(Collections.singletonList(playerInfoData));

        playerInfoWrapper.sendPacket(affectedPlayer);
    }

    public PlayerInfoAction getAction()
    {
        return handle.getPlayerInfoAction().read(0);
    }

    public void setAction(PlayerInfoAction value)
    {
        handle.getPlayerInfoAction().write(0, value);
    }

    public List<PlayerInfoData> getData()
    {
        return handle.getPlayerInfoDataLists().read(0);
    }

    public void setData(List<PlayerInfoData> value)
    {
        handle.getPlayerInfoDataLists().write(0, value);
    }
}
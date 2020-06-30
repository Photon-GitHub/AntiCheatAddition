package de.photon.aacadditionpro.util.playersimulation.displayinformation;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.aacadditionpro.util.packetwrappers.server.WrapperPlayServerPlayerInfo;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.entity.Player;

import java.util.Collections;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DisplayInformation
{
    /**
     * This method updates the player information, and thus the tablist of a player.
     *
     * @param action         the {@link com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction} that will be executed.
     * @param gameProfile    the {@link WrappedGameProfile} of the player whose information will be updated.<br>
     *                       Use {@link WrappedGameProfile#fromPlayer(Player)} in order to get a {@link WrappedGameProfile} from a {@link Player}.
     * @param ping           the new ping of the updated {@link Player}.
     * @param gameMode       the {@link com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode} of the updated {@link Player}.
     *                       Use {@link de.photon.aacadditionpro.util.server.ServerUtil#getPing(Player)} to get the ping of a {@link Player}.
     * @param displayName    the new displayName of the updated {@link Player}
     * @param affectedPlayer the {@link Player} who will see the updated information as the packet is sent to him.
     */
    public static void updatePlayerInformation(final EnumWrappers.PlayerInfoAction action, final WrappedGameProfile gameProfile, final int ping, final EnumWrappers.NativeGameMode gameMode, final WrappedChatComponent displayName, final Player affectedPlayer)
    {
        final PlayerInfoData playerInfoData = new PlayerInfoData(gameProfile, ping, gameMode, displayName);

        final WrapperPlayServerPlayerInfo playerInfoWrapper = new WrapperPlayServerPlayerInfo();
        playerInfoWrapper.setAction(action);
        playerInfoWrapper.setData(Collections.singletonList(playerInfoData));

        playerInfoWrapper.sendPacket(affectedPlayer);
    }
}

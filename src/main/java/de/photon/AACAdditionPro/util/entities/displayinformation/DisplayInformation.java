package de.photon.AACAdditionPro.util.entities.displayinformation;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.AACAdditionPro.util.entities.ClientsidePlayerEntity;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPlayerInfo;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class DisplayInformation
{
    /**
     * This updates the {@link Team} of a {@link ClientsidePlayerEntity} to avoid bypasses as of no {@link Team} or
     * the same {@link Team} as the observed {@link Player}.
     *
     * @param clientsidePlayerEntity the {@link ClientsidePlayerEntity} the {@link Team} will be applied to
     */
    public static void applyTeams(final ClientsidePlayerEntity clientsidePlayerEntity)
    {
        final Set<Team> teamsWithPlayers = new HashSet<>();
        for (final Team team1 : clientsidePlayerEntity.getObservedPlayer().getScoreboard().getTeams())
        {
            if ((team1.getEntries().size() > 1 || !team1.getEntries().contains(clientsidePlayerEntity.getGameProfile().getName())) && team1.getSuffix().isEmpty())
            {
                teamsWithPlayers.add(team1);
            }
        }

        if (teamsWithPlayers.isEmpty())
        {
            for (final Team team1 : clientsidePlayerEntity.getObservedPlayer().getScoreboard().getTeams())
            {
                if (team1.getSuffix().isEmpty())
                {
                    teamsWithPlayers.add(team1);
                }
            }
        }

        if (clientsidePlayerEntity.getCurrentTeam() == null || !teamsWithPlayers.contains(clientsidePlayerEntity.getCurrentTeam()))
        {
            for (Team team : teamsWithPlayers)
            {
                if (!team.getEntries().contains(clientsidePlayerEntity.getObservedPlayer().getName()))
                {
                    try
                    {
                        // The Entity will automatically leave its current team, no extra method call required.
                        clientsidePlayerEntity.joinTeam(team);
                        // No further team joining.
                        return;
                    } catch (IllegalArgumentException entityIsNull)
                    {
                        // Here the entity is null and thus additional measures are not required.
                        return;
                    } catch (IllegalStateException ignore)
                    {
                        // Here the team is no longer present -> loop through the other teams.
                    }
                }
            }
        }
    }


    /**
     * This method updates the player information, and thus the tablist of a player.
     *
     * @param action         the {@link com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction} that will be executed.
     * @param gameProfile    the {@link WrappedGameProfile} of the player whose information will be updated.<br>
     *                       Use {@link WrappedGameProfile#fromPlayer(Player)} in order to get a {@link WrappedGameProfile} from a {@link Player}.
     * @param ping           the new ping of the updated {@link Player}.
     * @param gameMode       the {@link com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode} of the updated {@link Player}.
     *                       Use {@link me.konsolas.aac.api.AACAPI#getPing(Player)} to get the ping of a {@link Player}.
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

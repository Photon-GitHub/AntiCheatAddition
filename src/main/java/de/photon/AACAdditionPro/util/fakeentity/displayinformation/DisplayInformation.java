package de.photon.AACAdditionPro.util.fakeentity.displayinformation;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.util.fakeentity.ClientsidePlayerEntity;
import de.photon.AACAdditionPro.util.files.configs.ConfigUtils;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPlayerInfo;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public final class DisplayInformation
{
    private static final List<String> preferredTeamNames = ConfigUtils.loadStringOrStringList(ModuleType.KILLAURA_ENTITY.getConfigString() + ".behaviour.team.preferred_teams");

    /**
     * This updates the {@link Team} of a {@link ClientsidePlayerEntity} to avoid bypasses as of no {@link Team} or
     * the same {@link Team} as the observed {@link Player}.
     *
     * @param clientsidePlayerEntity the {@link ClientsidePlayerEntity} the {@link Team} will be applied to
     */
    public static void applyTeams(final ClientsidePlayerEntity clientsidePlayerEntity)
    {
        // Try to search for teams with players inside to make sure the entity is placed in a real enemy team if
        // possible.
        final SortedSet<Team> possibleTeams = new TreeSet<>((o1, o2) -> {
            boolean o1Preferred = preferredTeamNames.contains(o1.getName());
            boolean o2Preferred = preferredTeamNames.contains(o2.getName());

            // Preferred team handling
            if (o1Preferred && !o2Preferred)
            {
                return 1;
            }
            else if (!o1Preferred && o2Preferred)
            {
                return -1;
            }
            return Integer.compare(o1.getSize(), o2.getSize());
        });

        for (Team team : clientsidePlayerEntity.getObservedPlayer().getScoreboard().getTeams())
        {
            // Don't add teams with suffix and the team of the observed player.
            if (team.getSuffix().isEmpty() && !team.getEntries().contains(clientsidePlayerEntity.getGameProfile().getName()))
            {
                possibleTeams.add(team);
            }
        }

        // Currently in no team
        if (clientsidePlayerEntity.getCurrentTeam() == null ||
            // The observed player has joined the team of the entity.
            !possibleTeams.contains(clientsidePlayerEntity.getCurrentTeam()))
        {
            for (Team possibleTeam : possibleTeams)
            {
                try
                {
                    // The Entity will automatically leave its current team, no extra method call required.
                    clientsidePlayerEntity.joinTeam(possibleTeam);
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

package de.photon.AACAdditionPro.util.entities.displayinformation;

import com.comphenix.protocol.wrappers.EnumWrappers;
import com.comphenix.protocol.wrappers.PlayerInfoData;
import de.photon.AACAdditionPro.util.entities.ClientsidePlayerEntity;
import de.photon.AACAdditionPro.util.packetwrappers.WrapperPlayServerPlayerInfo;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

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
        for (final Team team1 : clientsidePlayerEntity.getObservedPlayer().getScoreboard().getTeams()) {
            if ((team1.getEntries().size() > 1 || !team1.getEntries().contains(clientsidePlayerEntity.getGameProfile().getName())) && team1.getSuffix().isEmpty()) {
                teamsWithPlayers.add(team1);
            }
        }

        if (teamsWithPlayers.isEmpty()) {
            for (final Team team1 : clientsidePlayerEntity.getObservedPlayer().getScoreboard().getTeams()) {
                if (team1.getSuffix().isEmpty()) {
                    teamsWithPlayers.add(team1);
                }
            }
        }

        if (clientsidePlayerEntity.getCurrentTeam() == null || !teamsWithPlayers.contains(clientsidePlayerEntity.getCurrentTeam())) {
            final Iterator<Team> iterator = teamsWithPlayers.iterator();
            if (iterator.hasNext()) {
                Team team;

                do {
                    team = iterator.next();
                }
                while (iterator.hasNext() && team.getEntries().contains(clientsidePlayerEntity.getObservedPlayer().getName()));

                if (clientsidePlayerEntity.getCurrentTeam() != null) {
                    clientsidePlayerEntity.getCurrentTeam().removeEntry(clientsidePlayerEntity.getGameProfile().getName());
                }

                team.addEntry(clientsidePlayerEntity.getGameProfile().getName());
                clientsidePlayerEntity.setCurrentTeam(team);
            }
        }
    }

    /**
     * Fakes a changing ping for a {@link ClientsidePlayerEntity}
     */
    public static void updatePing(final ClientsidePlayerEntity clientsidePlayerEntity)
    {
        if (clientsidePlayerEntity.isValid()) {
            ping(clientsidePlayerEntity, clientsidePlayerEntity.getObservedPlayer(), 21 + ThreadLocalRandom.current().nextInt(4));
        }
    }

    /**
     * The real {@link java.lang.reflect.Method} that handles the ping-changing once the given {@link ClientsidePlayerEntity} is confirmed as valid.
     *
     * @param clientsidePlayerEntity the {@link ClientsidePlayerEntity} which ping should be changed
     * @param observedPlayer         the {@link Player} that is observed by the entity and should recognize the ping-change
     * @param ping                   the new ping of the {@link ClientsidePlayerEntity}
     */
    private static void ping(final ClientsidePlayerEntity clientsidePlayerEntity, final Player observedPlayer, final int ping)
    {
        if (clientsidePlayerEntity.isSpawned()) {
            // Send player info first
            final PlayerInfoData playerInfoData = new PlayerInfoData(clientsidePlayerEntity.getGameProfile(), ping, EnumWrappers.NativeGameMode.SURVIVAL, null);

            final WrapperPlayServerPlayerInfo playerInfoWrapper = new WrapperPlayServerPlayerInfo();
            playerInfoWrapper.setAction(EnumWrappers.PlayerInfoAction.UPDATE_LATENCY);
            playerInfoWrapper.setData(Collections.singletonList(playerInfoData));
            playerInfoWrapper.sendPacket(observedPlayer);
        }
    }
}

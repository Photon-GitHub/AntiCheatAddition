package de.photon.AACAdditionPro.util.entities.displayinformation;

import de.photon.AACAdditionPro.util.entities.ClientsidePlayerEntity;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

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
                    } catch (IllegalStateException ignore)
                    {
                        // This is ignored as of the potentially unregistered scoreboard components.
                    }
                    return;
                }
            }
        }
    }
}

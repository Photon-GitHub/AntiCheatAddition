package de.photon.aacadditionpro.util.violationlevels;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.util.commands.CommandUtils;
import de.photon.aacadditionpro.util.commands.Placeholders;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TeamViolationLevelManagement extends ViolationLevelManagement
{
    public TeamViolationLevelManagement(final ModuleType moduleType, final long decrease_delay)
    {
        super(moduleType, decrease_delay);
    }

    /**
     * Get all the ViolationLevels of all given {@link UUID}s
     *
     * @param uuids the {@link UUID}s of the {@link Player}s whose VLs should be added up
     *
     * @return the sum of the VLs of all given {@link UUID}s
     */
    public int getTeamVl(final List<UUID> uuids)
    {
        int vlSum = 0;
        for (final UUID uuid : uuids) {
            vlSum += this.getVL(uuid);
        }
        return vlSum;
    }

    /**
     * Flags a whole team.
     *
     * @param players     the players that should be flagged.
     * @param cancel_vl   the ViolationLevel up from which onCancel is run. Set to -1 to disable
     * @param onCancel    a {@link Runnable} that is executed if the vl is higher that cancel_vl
     * @param specialCode a {@link Runnable} to define special code such as critical_vl
     */

    public void flagTeam(final List<Player> players, final int cancel_vl, final Runnable onCancel, final Runnable specialCode)
    {
        this.flagTeam(players, 1, cancel_vl, onCancel, specialCode);
    }

    /**
     * Flags a whole team.
     *
     * @param players     the players that should be flagged.
     * @param vl_increase how much the vl should be increased.
     * @param cancel_vl   the ViolationLevel up from which onCancel is run. Set to -1 to disable
     * @param onCancel    a {@link Runnable} that is executed if the vl is higher that cancel_vl
     * @param specialCode a {@link Runnable} to define special code such as critical_vl
     */

    public void flagTeam(final List<Player> players, final int vl_increase, final int cancel_vl, final Runnable onCancel, final Runnable specialCode)
    {
        final List<UUID> uuids = new ArrayList<>(players.size());

        for (final Player player : players) {
            uuids.add(player.getUniqueId());
            this.flag(player, vl_increase, cancel_vl, onCancel, specialCode);
        }

        punishTeam(players, this.getTeamVl(uuids));
    }

    @Override
    public void setVL(final Player player, final int newVl)
    {
        violationLevels.put(player.getUniqueId(), newVl);
    }

    /**
     * Used to execute the command that are defined in the config section CHECK_NAME.thresholds
     *
     * @param playersOfTeam the {@link Player}s that should be punished and that should be used to apply the placeholders
     * @param teamVL        the ViolationLevel of the team
     */
    private void punishTeam(final List<Player> playersOfTeam, final Integer teamVL)
    {
        // Only schedule the command execution if the plugin is loaded
        if (AACAdditionPro.getInstance().isLoaded() && AACAdditionPro.getInstance().getAacapi() == null) {
            // Find the biggest element below teamVL
            final int index = this.thresholds.getToIndex(teamVL);
            if (index >= 0) {
                // Execute the commands
                for (final String command : this.thresholds.getThresholds().get(index).getCommandList()) {
                    // Sync command execution
                    CommandUtils.executeCommand(Placeholders.replacePlaceholders(command, playersOfTeam, playersOfTeam.get(0).getWorld()));
                }
            }
        }
    }
}

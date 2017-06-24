package de.photon.AACAdditionPro.util.storage.management;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.util.commands.CommandUtils;
import de.photon.AACAdditionPro.util.commands.Placeholders;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

public class TeamViolationLevelManagement extends ViolationLevelManagement
{
    public TeamViolationLevelManagement(final AdditionHackType additionHackType, final long decrease_delay)
    {
        super(additionHackType, decrease_delay);
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
        this.flagTeam(players, vl_increase, cancel_vl, onCancel, specialCode, true);
    }

    /**
     * Flags a whole team.
     *
     * @param players     the players that should be flagged.
     * @param vl_increase how much the vl should be increased.
     * @param cancel_vl   the ViolationLevel up from which onCancel is run. Set to -1 to disable
     * @param onCancel    a {@link Runnable} that is executed if the vl is higher that cancel_vl
     * @param specialCode a {@link Runnable} to define special code such as critical_vl
     * @param punish      should the affected team be punished
     */

    public void flagTeam(final List<Player> players, final int vl_increase, final int cancel_vl, final Runnable onCancel, final Runnable specialCode, boolean punish)
    {
        final List<UUID> uuids = new ArrayList<>(players.size());

        for (final Player player : players) {
            uuids.add(player.getUniqueId());
            this.flag(player, vl_increase, cancel_vl, onCancel, specialCode);
        }

        punishTeam(players, this.getTeamVl(uuids));
        /*final UUID[] uuids = new UUID[players.length];

        for (short s = 0; s < players.length; s++) {
            this.flag(players[s], vl_increase, cancel_vl, onCancel, specialCode);
            uuids[s] = players[s].getUniqueId();
        }

        punishTeam(this.getTeamVl(uuids), players);*/
    }

    @Override
    public void setVL(final Player player, final int newVl)
    {
        this.setVL(player, newVl, false);
    }

    /**
     * Used to execute the command that are defined in the config section CHECK_NAME.thresholds
     *
     * @param playersOfTeam the {@link Player}s that should be punished and that should be used to apply the placeholders
     * @param teamVL        the ViolationLevel of the team
     */
    public void punishTeam(final List<Player> playersOfTeam, final Integer teamVL)
    {
        Integer maxThreshold = -1;

        final Enumeration<Integer> keys = thresholds.keys();
        Integer currentElement;

        while (keys.hasMoreElements()) {
            currentElement = keys.nextElement();

            if (currentElement > maxThreshold && currentElement < teamVL) {
                maxThreshold = currentElement;
            }
        }

        if (maxThreshold > -1) {
            for (final String s : thresholds.get(maxThreshold)) {

                if (s == null) {
                    throw new RuntimeException("Violation-Command is null.");
                }

                final String realCommand = Placeholders.applyPlaceholders(s, playersOfTeam);
                //Sync command execution
                Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> CommandUtils.executeCommand(realCommand));
            }
        }
    }
}

package de.photon.AACAdditionPro.util.storage.management;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.events.PlayerAdditionViolationCommandEvent;
import de.photon.AACAdditionPro.events.PlayerAdditionViolationEvent;
import de.photon.AACAdditionPro.util.commands.CommandUtils;
import de.photon.AACAdditionPro.util.commands.Placeholders;
import de.photon.AACAdditionPro.util.files.ConfigUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ViolationLevelManagement implements Listener
{
    /**
     * The {@link Map} violation-levels of all the players.
     */
    private final ConcurrentMap<UUID, Integer> violationLevels = new ConcurrentHashMap<>();

    /**
     * The {@link Map} of the command that are defined in the config at certain violation-levels.
     * This is automatically a {@link ConcurrentHashMap} as of the
     */
    final ConcurrentMap<Integer, List<String>> thresholds;

    /**
     * The {@link ModuleType} of the handler, used for reload
     */
    private final ModuleType moduleType;

    public ViolationLevelManagement(final ModuleType moduleType, final long decreaseDelay)
    {
        // The ModuleType of the check
        this.moduleType = moduleType;

        // Listener registration as of the PlayerQuitEvent
        AACAdditionPro.getInstance().registerListener(this);

        // Load the thresholds
        thresholds = ConfigUtils.loadThresholds(moduleType.getConfigString() + ".thresholds");

        //The vl-decrease
        Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AACAdditionPro.getInstance(), () -> violationLevels.forEach(
                        (uuid, vl) ->
                        {
                            final int newVl = vl - 1;

                            if (newVl > 0)
                            {
                                violationLevels.put(uuid, newVl);
                            }
                            else
                            {
                                violationLevels.remove(uuid);
                            }
                        }), 0L, decreaseDelay);
    }

    /**
     * Flags a {@link Player} in the violationLevelManagement with the amount of vl_increase
     *
     * @param player      the player that should be flagged.
     * @param cancel_vl   the ViolationLevel up from which onCancel is run. Set to -1 to disable
     * @param onCancel    a {@link Runnable} that is executed if the vl is higher that cancel_vl
     * @param specialCode a {@link Runnable} to define special code such as critical_vl
     */
    public void flag(final Player player, final int cancel_vl, final Runnable onCancel, final Runnable specialCode)
    {
        flag(player, 1, cancel_vl, onCancel, specialCode);
    }

    /**
     * Flags a {@link Player} in the violationLevelManagement with the amount of vl_increase
     *
     * @param player      the player that should be flagged.
     * @param vl_increase how much the vl should be increased.
     * @param cancel_vl   the ViolationLevel up from which onCancel is run. Set to -1 to disable
     * @param onCancel    a {@link Runnable} that is executed if the vl is higher that cancel_vl
     * @param specialCode a {@link Runnable} to define special code such as critical_vl
     */
    public void flag(final Player player, final int vl_increase, final int cancel_vl, final Runnable onCancel, final Runnable specialCode)
    {
        // Only create the event if it should be called.
        final PlayerAdditionViolationEvent playerAdditionViolationEvent = new PlayerAdditionViolationEvent(
                player,
                this.moduleType,
                this.getVL(player.getUniqueId()) + vl_increase,
                this.moduleType.getViolationMessage()
        );

        // Call the event
        AACAdditionPro.getInstance().getServer().getPluginManager().callEvent(playerAdditionViolationEvent);


        if (!playerAdditionViolationEvent.isCancelled())
        {
            this.addVL(player, vl_increase);

            //Cancel
            if (cancel_vl > 0 && cancel_vl < this.getVL(player.getUniqueId()))
            {
                onCancel.run();
            }

            specialCode.run();
        }
    }

    /**
     * @param uuid the {@link UUID} of the {@link Player} whose vl should be returned.
     * @return the vl of the given uuid.
     */
    public final int getVL(final UUID uuid)
    {
        final Integer vl = violationLevels.get(uuid);
        // If the vl would be null 0 is returned.
        return vl == null ?
               0 :
               vl;
    }

    /**
     * Sets the vl of a player.
     *
     * @param player the {@link Player} whose vl should be set
     * @param newVl  the new vl of the player.
     * @param punish whether punishing the player should be tried after setting the new value
     */
    void setVL(final Player player, final int newVl, final boolean punish)
    {
        final int oldVl = getVL(player.getUniqueId());

        // A value smaller than 0 can be removed
        if (newVl > 0 &&
            // If the player is offline he can be removed
            player.isOnline())
        {
            violationLevels.put(player.getUniqueId(), newVl);

            // setVL is also called when decreasing the vl
            // thus we must prevent double punishment
            if (punish && oldVl < newVl)
            {
                this.punishPlayer(player, oldVl, newVl);
            }
        }
        else
        {
            violationLevels.remove(player.getUniqueId());
        }
    }

    /**
     * Sets the vl of a player.
     *
     * @param player the {@link Player} whose vl should be set
     * @param newVl  the new vl of the player.
     */
    public void setVL(final Player player, final int newVl)
    {
        this.setVL(player, newVl, true);
    }

    /**
     * Adds an {@link Integer} to the vl of a player. The number can be negative, this will decrease the vl then.
     *
     * @param player the {@link Player} whose vl should be set
     * @param vl     the new vl of the player.
     */
    private void addVL(final Player player, final Integer vl)
    {
        this.setVL(player, this.getVL(player.getUniqueId()) + vl);
    }

    /**
     * Used to execute the command that are defined in the config section CHECK_NAME.thresholds
     *
     * @param player the {@link Player} that should be punished and that should be used to apply the placeholders
     * @param fromvl the last vl of the player before the addition and the searching-range for command.
     */
    private void punishPlayer(final Player player, final int fromvl, final int toVl)
    {
        // Iterate through all the keys
        for (final Integer key : thresholds.keySet())
        {

            // If the key should be applied here
            if (key > fromvl && key <= toVl)
            {

                // Iterate through all the commands that are presented in the threshold of key
                for (final String s : thresholds.get(key))
                {

                    // Command cannot be null as of the new loading process.
                    final String realCommand = Placeholders.applyPlaceholders(s, player);

                    // Only schedule the command execution if the plugin is loaded
                    if (AACAdditionPro.getInstance().isLoaded())
                    {

                        // Calling of the event + Sync command execution
                        CommandUtils.executeCommand(new PlayerAdditionViolationCommandEvent(player, realCommand, this.moduleType));
                    }
                }
            }
        }
    }

    @EventHandler
    public void on(final PlayerQuitEvent event)
    {
        violationLevels.remove(event.getPlayer().getUniqueId());
    }
}

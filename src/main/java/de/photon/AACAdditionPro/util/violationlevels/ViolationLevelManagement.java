package de.photon.AACAdditionPro.util.violationlevels;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.events.PlayerAdditionViolationEvent;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.util.commands.CommandUtils;
import de.photon.AACAdditionPro.util.files.configs.ConfigUtils;
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
    protected final ConcurrentMap<UUID, Integer> violationLevels = new ConcurrentHashMap<>();

    /**
     * The {@link Map} of the command that are defined in the config at certain violation-levels.
     * This is automatically a {@link ConcurrentHashMap} as of the
     */
    final ConcurrentMap<Integer, List<String>> thresholds;

    /**
     * The {@link ModuleType} of the handler, used for reload
     */
    private final ModuleType moduleType;

    /**
     * Create a new {@link ViolationLevelManagement}
     *
     * @param moduleType    the {@link ModuleType} of the module this {@link ViolationLevelManagement} is being used by.
     * @param decreaseDelay the time in ticks until the vl of a player is decreased by one. If this is negative no decrease will happen.
     */
    public ViolationLevelManagement(final ModuleType moduleType, final long decreaseDelay)
    {
        // The ModuleType of the check
        this.moduleType = moduleType;

        ModuleType.VL_MODULETYPES.add(moduleType);

        // Listener registration as of the PlayerQuitEvent
        AACAdditionPro.getInstance().registerListener(this);

        // Load the thresholds
        thresholds = ConfigUtils.loadThresholds(moduleType.getConfigString() + ".thresholds");

        // Might need to have a vl manager without vl decrease
        if (decreaseDelay > 0)
        {
            //The vl-decrease
            Bukkit.getScheduler().scheduleSyncRepeatingTask(
                    AACAdditionPro.getInstance(), () -> {
                        for (Map.Entry<UUID, Integer> entry : violationLevels.entrySet())
                        {
                            final int newVl = entry.getValue() - 1;
                            if (newVl > 0)
                            {
                                entry.setValue(newVl);
                            }
                            else
                            {
                                violationLevels.remove(entry.getKey());
                            }
                        }
                    }, 0L, decreaseDelay);
        }
    }

    /**
     * Flags a {@link Player} in the violationLevelManagement with the amount of vl_increase
     *
     * @param player      the player that should be flagged.
     * @param cancel_vl   the ViolationLevel up from which onCancel is run. Set to -1 to disable
     * @param onCancel    a {@link Runnable} that is executed if the vl is higher that cancel_vl
     * @param specialCode a {@link Runnable} to define special code such as critical_vl. Contrary to normal code it is only run if the event is not cancelled.
     */
    public void flag(final Player player, final int cancel_vl, final Runnable onCancel, final Runnable specialCode)
    {
        flag(player, 1, cancel_vl, onCancel, specialCode);
    }

    /**
     * Flags a {@link Player} in the violationLevelManagement with the amount of vlIncrease
     *
     * @param player      the player that should be flagged.
     * @param vlIncrease  how much the vl should be increased.
     * @param cancelVl    the ViolationLevel up from which onCancel is run. Set to -1 to disable
     * @param onCancel    a {@link Runnable} that is executed if the vl is higher that cancelVl
     * @param specialCode a {@link Runnable} to define special code such as critical_vl. Contrary to normal code it is only run if the event is not cancelled.
     */
    public void flag(final Player player, final int vlIncrease, final int cancelVl, final Runnable onCancel, final Runnable specialCode)
    {
        // Prevent unnecessary flagging.
        if (vlIncrease <= 0)
        {
            return;
        }

        // Only create the event if it should be called.
        final PlayerAdditionViolationEvent playerAdditionViolationEvent = new PlayerAdditionViolationEvent(
                player,
                this.moduleType,
                this.getVL(player.getUniqueId()) + vlIncrease,
                this.moduleType.getViolationMessage()
        );

        // Call the event
        AACAdditionPro.getInstance().getServer().getPluginManager().callEvent(playerAdditionViolationEvent);


        if (!playerAdditionViolationEvent.isCancelled())
        {
            this.addVL(player, vlIncrease);

            //Cancel
            if (cancelVl > 0 && cancelVl <= this.getVL(player.getUniqueId()))
            {
                onCancel.run();
            }

            specialCode.run();
        }
    }

    /**
     * @param uuid the {@link UUID} of the {@link Player} whose vl should be returned.
     *
     * @return the vl of the given uuid.
     */
    public final int getVL(final UUID uuid)
    {
        return violationLevels.getOrDefault(uuid, 0);
    }

    /**
     * Sets the vl of a player.
     *
     * @param player the {@link Player} whose vl should be set
     * @param newVl  the new vl of the player.
     */
    public void setVL(final Player player, final int newVl)
    {
        final int oldVl = this.getVL(player.getUniqueId());

        // A value smaller than 0 can be removed
        if (newVl > 0 &&
            // If the player is offline he can be removed
            player.isOnline())
        {
            violationLevels.put(player.getUniqueId(), newVl);

            // setVL is also called when decreasing the vl
            // thus we must prevent double punishment
            if (oldVl < newVl)
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
     * Adds an {@link Integer} to the vl of a player. The number can be negative, this will decrease the vl then.
     *
     * @param player the {@link Player} whose vl should be set
     * @param vl     the vl to be added to the current vl.
     */
    private void addVL(final Player player, final int vl)
    {
        this.setVL(player, this.getVL(player.getUniqueId()) + vl);
    }

    /**
     * Used to execute the command that are defined in the config section CHECK_NAME.thresholds
     *
     * @param player the {@link Player} that should be punished and that should be used to apply the placeholders
     * @param fromVl the last vl of the player before the addition and the searching-range for command.
     */
    private void punishPlayer(final Player player, final int fromVl, final int toVl)
    {
        // Only schedule the command execution if the plugin is loaded
        if (AACAdditionPro.getInstance().isLoaded())
        {
            // Iterate through all the keys
            thresholds.forEach((threshold, commandList) -> {
                // If the key should be applied here
                if (threshold > fromVl && threshold <= toVl)
                {
                    // Iterate through all the commands that are presented in the threshold of key
                    for (final String command : commandList)
                    {
                        // Calling of the event + Sync command execution
                        CommandUtils.executeCommandWithPlaceholders(command, player, this.moduleType, (double) toVl);
                    }
                }
            });
        }
    }

    @EventHandler
    public void on(final PlayerQuitEvent event)
    {
        violationLevels.remove(event.getPlayer().getUniqueId());
    }
}

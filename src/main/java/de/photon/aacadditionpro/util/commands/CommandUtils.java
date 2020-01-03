package de.photon.aacadditionpro.util.commands;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.events.PlayerAdditionViolationCommandEvent;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.util.VerboseSender;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public final class CommandUtils
{
    private CommandUtils() {}

    /**
     * Applies all the {@link Placeholders} to the given command and invokes executeCommand afterwards.
     *
     * @param player  the player that should be used for the {@link Placeholders}
     * @param command the command with placeholders that should be executed
     */
    public static void executeCommandWithPlaceholders(final String command, final Player player, final ModuleType moduleType)
    {
        PlayerAdditionViolationCommandEvent.createAndCallCommandEvent(player, Placeholders.replacePlaceholders(command, player), moduleType)
                                           .runIfUncancelled(commandEvent -> executeCommand(commandEvent.getCommand()));
    }

    /**
     * This executes the command synchronously and sends an error message via {@link VerboseSender} if something went wrong.
     * No {@link Placeholders} are allowed to exist in this method, use executeCommandWithPlaceholders() for this.
     *
     * @param command the command that will be run from console.
     */
    public static void executeCommand(final String command)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(
                AACAdditionPro.getInstance(),
                () -> {
                    //Try catch to prevent console errors if a command couldn't be executed, e.g. if the player has left.
                    try {
                        Bukkit.dispatchCommand(AACAdditionPro.getInstance().getServer().getConsoleSender(), command);
                        VerboseSender.getInstance().sendVerboseMessage(ChatColor.GOLD + "Executed command: " + command);
                    } catch (final Exception e) {
                        VerboseSender.getInstance().sendVerboseMessage("Could not execute command /" + command, true, true);
                    }
                });
    }
}
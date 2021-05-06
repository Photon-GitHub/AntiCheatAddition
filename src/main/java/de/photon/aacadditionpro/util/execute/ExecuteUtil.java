package de.photon.aacadditionpro.util.execute;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ExecuteUtil
{
    /**
     * This executes the command synchronously and sends an error message via {@link DebugSender} if something went wrong.
     * No {@link Placeholders} are allowed to exist in this method, use executeCommandWithPlaceholders() for this.
     *
     * @param command the command that will be run from console.
     */
    public static void executeCommand(final String command)
    {
        Bukkit.getScheduler().runTask(
                AACAdditionPro.getInstance(),
                () -> {
                    //Try catch to prevent console errors if a command couldn't be executed, e.g. if the player has left.
                    try {
                        Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), command);
                        DebugSender.getInstance().sendDebug(ChatColor.GOLD + "Executed command: " + command);
                    } catch (final Exception e) {
                        DebugSender.getInstance().sendDebug("Could not execute command /" + command, true, true);
                    }
                });
    }
}
package de.photon.aacadditionpro.commands.subcommands;

import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.commands.CommandAttributes;
import de.photon.aacadditionpro.commands.InternalPlayerCommand;
import de.photon.aacadditionpro.commands.TabCompleteSupplier;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.messaging.ChatMessage;
import lombok.val;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Queue;

public class DebugCommand extends InternalPlayerCommand
{
    private static final String ENABLED_MESSAGE = "Debug " + ChatColor.DARK_GREEN + "enabled";
    private static final String DISABLED_MESSAGE = "Debug " + ChatColor.RED + "disabled";


    public DebugCommand()
    {
        super("debug", CommandAttributes.builder()
                                        .minArguments(0)
                                        .maxArguments(1)
                                        .addCommandHelpLine("Used to toggle debug messages on and off.")
                                        .setPermission(InternalPermission.DEBUG)
                                        .build(), TabCompleteSupplier.builder().constants("on", "off"));
    }

    @Override
    protected void execute(Player sender, Queue<String> arguments)
    {
        val user = User.getUser(sender.getUniqueId());
        if (user == null) return;

        boolean toggleTo;
        if (arguments.peek() != null) {
            switch (arguments.peek().toLowerCase()) {
                case "on":
                    toggleTo = true;
                    break;
                case "off":
                    toggleTo = false;
                    break;
                default:
                    toggleTo = !user.hasDebug();
                    break;
            }
        } else {
            toggleTo = !user.hasDebug();
        }

        user.setDebug(toggleTo);
        ChatMessage.sendMessage(sender, toggleTo ? ENABLED_MESSAGE : DISABLED_MESSAGE);
    }
}

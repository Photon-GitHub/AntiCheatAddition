package de.photon.aacadditionpro.commands.subcommands;

import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.commands.CommandAttributes;
import de.photon.aacadditionpro.commands.InternalPlayerCommand;
import de.photon.aacadditionpro.commands.TabCompleteSupplier;
import de.photon.aacadditionpro.util.messaging.ChatMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Queue;

public class VerboseCommand extends InternalPlayerCommand
{
    public VerboseCommand()
    {
        super("verbose", CommandAttributes.builder()
                                          .minArguments(0)
                                          .maxArguments(1)
                                          .addCommandHelpLine("Used to toggle verbose messages on and off.")
                                          .setPermission(InternalPermission.AAC_VERBOSE)
                                          .build(), TabCompleteSupplier.builder().constants("on", "off").commandHelp());
    }

    /**
     * Used to send the right message
     *
     * @param sender  the {@link CommandSender} of the command, the message will be sent to him
     * @param enabled the message-format (was Verbose disabled or enabled)
     */
    private static void sendToggleMessage(final CommandSender sender, final boolean enabled)
    {
        ChatMessage.sendMessage(sender, "Verbose " + (enabled ?
                                                      (ChatColor.DARK_GREEN + "enabled") :
                                                      (ChatColor.RED + "disabled")));
    }

    @Override
    protected void execute(Player sender, Queue<String> arguments)
    {
        final User user = UserManager.getUser(sender.getUniqueId());

        if (user == null) {
            return;
        }

        boolean toggleTo = !UserManager.isVerbose(user);
        if (arguments.peek() != null) {
            switch (arguments.peek().toLowerCase()) {
                case "on":
                    toggleTo = true;
                    break;
                case "off":
                    toggleTo = false;
                    break;
                default:
                    break;
            }
        }

        //Toggle mode
        UserManager.setVerbose(user, toggleTo);
        sendToggleMessage(sender, toggleTo);
    }
}

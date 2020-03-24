package de.photon.aacadditionpro.command.subcommands;

import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.command.CommandAttributes;
import de.photon.aacadditionpro.command.InternalPlayerCommand;
import de.photon.aacadditionpro.command.TabCompleteSupplier;
import de.photon.aacadditionpro.olduser.UserManager;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.messaging.ChatMessage;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Queue;

public class VerboseCommand extends InternalPlayerCommand
{
    public VerboseCommand()
    {
        super("verbose", InternalPermission.AAC_VERBOSE,
              CommandAttributes.builder()
                               .minArguments(0)
                               .maxArguments(1)
                               .addCommandHelpLine("Used to toggle the verbose messages on and off for oneself.")
                               .build(),
              Collections.emptySet(),
              TabCompleteSupplier.builder().constants("on", "off"));
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

    /**
     * Used to send the right message
     *
     * @param sender  the {@link CommandSender} of the command, the message will be sent to him
     * @param enabled the message-format (was Verbose disabled or enabled)
     */
    private static void sendToggleMessage(final CommandSender sender, final boolean enabled)
    {
        ChatMessage.sendInfoMessage(sender, ChatColor.GOLD, "Verbose " + (enabled ?
                                                                          (ChatColor.DARK_GREEN + "enabled") :
                                                                          (ChatColor.RED + "disabled")));
    }
}

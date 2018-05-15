package de.photon.AACAdditionPro.command.subcommands;

import com.google.common.collect.ImmutableList;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Queue;

public class VerboseCommand extends InternalCommand
{
    public VerboseCommand()
    {
        super("verbose", InternalPermission.VERBOSE, true, (byte) 0, (byte) 1);
    }

    /**
     * Used to send the right message
     *
     * @param sender  the {@link CommandSender} of the command, the message will be sent to him
     * @param enabled the message-format (was Verbose disabled or enabled)
     */
    private static void sendToggleMessage(final CommandSender sender, final boolean enabled)
    {
        sender.sendMessage(PREFIX + ChatColor.GOLD + "Verbose " + (enabled ?
                                                                   (ChatColor.DARK_GREEN + "enabled") :
                                                                   (ChatColor.RED + "disabled")
        ));
    }

    @Override
    protected void execute(CommandSender sender, Queue<String> arguments)
    {
        final User user = UserManager.getUser(((Player) sender).getUniqueId());

        if (user == null)
        {
            return;
        }

        boolean toggleTo = !user.verbose;
        if (arguments.peek() != null)
        {
            switch (arguments.peek().toLowerCase())
            {
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
        user.verbose = toggleTo;
        sendToggleMessage(sender, user.verbose);
    }

    @Override
    protected String[] getCommandHelp()
    {
        return new String[]{"Used to toggle the verbose messages on and off for oneself."};
    }

    @Override
    protected List<String> getTabPossibilities()
    {
        return ImmutableList.of("on", "off");
    }
}

package de.photon.AACAdditionPro.command.subcommands;

import de.photon.AACAdditionPro.Permissions;
import de.photon.AACAdditionPro.command.InternalCommand;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class VerboseCommand extends InternalCommand
{
    public VerboseCommand()
    {
        super("verbose",
              (byte) 0,
              true,
              Permissions.VERBOSE,
              "Used to toggle the verbose messages on and off for oneself."
             );
    }

    @Override
    protected void execute(final CommandSender sender, final LinkedList<String> arguments)
    {
        final User user = UserManager.getUser(((Player) sender).getUniqueId());

        if (user == null) {
            return;
        }

        if (arguments.size() == 1) {
            //on/off mode
            if ("on".equalsIgnoreCase(arguments.getFirst())) {
                //If is is already present the Set denies the adding
                user.verbose = true;
                sendToggleMessage(sender, true);
            } else if ("off".equalsIgnoreCase(arguments.getFirst())) {
                user.verbose = false;
                sendToggleMessage(sender, false);
            }
        } else {
            //Toggle mode
            user.verbose = !user.verbose;
            sendToggleMessage(sender, user.verbose);
        }
    }

    /**
     * Used to send the right message
     *
     * @param sender  the {@link CommandSender} of the command, the message will be sent to him
     * @param enabled the message-format (was Verbose disabled or enabled)
     */
    private static void sendToggleMessage(final CommandSender sender, final boolean enabled)
    {
        sender.sendMessage(prefix + ChatColor.GOLD + "Verbose " + (enabled ?
                                                                   (ChatColor.DARK_GREEN + "enabled") :
                                                                   (ChatColor.RED + "disabled")
        ));
    }

    @Override
    protected List<String> getTabPossibilities()
    {
        return new ArrayList<>(Arrays.asList("on", "off"));
    }
}

package de.photon.AACAdditionPro.command;

import de.photon.AACAdditionPro.Permissions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public abstract class InternalCommand
{

    protected static final String prefix = ChatColor.DARK_RED + "[AACAdditionPro] ";
    protected static final String playerNotFoundMessage = prefix + ChatColor.RED + "Player could not be found.";

    public final String commandName;
    private final byte minArguments;
    private final boolean onlyPlayers;
    private final Permissions permission;
    private final String[] commandHelp;

    protected InternalCommand(final String commandName,
                              final byte minArguments,
                              final boolean onlyPlayers,
                              final Permissions permission,
                              final String... commandHelp
                             )
    {
        this.commandName = commandName;
        this.minArguments = minArguments;
        this.onlyPlayers = onlyPlayers;
        this.permission = permission;
        this.commandHelp = commandHelp;
    }

    final void onSubCommand(final CommandSender sender, final LinkedList<String> arguments)
    {
        if (verifyCommand(sender, arguments)) {
            execute(sender, arguments);
        }
    }

    private boolean verifyCommand(final CommandSender sender, final LinkedList<String> arguments)
    {
        //Permission-Handling
        if (this.permission == null ||
            Permissions.hasPermission(sender, this.permission) ||
            sender.isOp())
        {
            //Command-Help
            if (arguments.size() == 1 && arguments.getFirst() != null && "?".equals(arguments.getFirst())) {
                for (final String s : commandHelp) {
                    sender.sendMessage(prefix + ChatColor.GOLD + s);
                }
                return true;
            }

            //Too few arguments
            if (arguments.size() < minArguments) {
                sender.sendMessage(prefix + ChatColor.RED + "Too few arguments.");
                return false;
            }

            //Only-Players
            if (this.onlyPlayers && !(sender instanceof Player)) {
                sender.sendMessage(prefix + ChatColor.RED + "This command can only be used ingame.");
                return false;
            }

            return true;
        }
        sender.sendMessage(prefix + ChatColor.RED + "You don't have permission to do this.");
        return false;
    }

    protected void execute(final CommandSender sender, final LinkedList<String> arguments) {}

    /**
     * @return a {@link HashSet} of the child-commands of the command or null if there are no children
     */
    protected Set<InternalCommand> getChildCommands()
    {
        return null;
    }

    protected List<String> getTabPossibilities()
    {
        final List<String> tabs = new ArrayList<>();
        for (final InternalCommand cmd : getChildCommands()) {
            tabs.add(cmd.commandName);
        }
        return tabs;
    }

    protected final void delegateToSubCommands(final CommandSender sender, final LinkedList<String> arguments)
    {
        for (final InternalCommand cmd : this.getChildCommands()) {
            if (cmd.commandName.equalsIgnoreCase(arguments.getFirst())) {
                arguments.removeFirst();
                cmd.onSubCommand(sender, arguments);
            }
        }
    }
}

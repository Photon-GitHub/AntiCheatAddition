package de.photon.AACAdditionPro.command;

import de.photon.AACAdditionPro.InternalPermission;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Queue;
import java.util.Set;

public abstract class InternalCommand
{
    protected static final String prefix = ChatColor.DARK_RED + "[AACAdditionPro] ";
    protected static final String playerNotFoundMessage = prefix + ChatColor.RED + "Player could not be found.";

    public final String name;
    private final InternalPermission permission;
    private final boolean onlyPlayers;
    private final byte minArguments;
    private final byte maxArguments;

    protected InternalCommand(String name, byte minArguments)
    {
        this(name, null, minArguments);
    }

    protected InternalCommand(String name, InternalPermission permission, byte minArguments)
    {
        this(name, permission, false, minArguments);
    }

    protected InternalCommand(String name, InternalPermission permission, boolean onlyPlayers, byte minArguments)
    {
        this(name, permission, onlyPlayers, minArguments, Byte.MAX_VALUE);
    }

    protected InternalCommand(String name, InternalPermission permission, boolean onlyPlayers, byte minArguments, byte maxArguments)
    {
        this.name = name;
        this.permission = permission;
        this.onlyPlayers = onlyPlayers;
        this.minArguments = minArguments;
        this.maxArguments = maxArguments;
    }

    void invokeCommand(CommandSender sender, Queue<String> arguments)
    {
        // No permission is set or the sender has the permission
        if (InternalPermission.hasPermission(sender, this.permission))
        {
            if (arguments.size() > 0)
            {
                // Help can be displayed at any time
                if (arguments.peek().equals("?"))
                {
                    for (String help : this.getCommandHelp())
                    {
                        sender.sendMessage(prefix + ChatColor.GOLD + help);
                    }
                }
                else
                {
                    Set<InternalCommand> childCommands = this.getChildCommands();
                    // Invoke command with arguments
                    if (childCommands == null)
                    {
                        this.executeIfAllowed(sender, arguments);
                    }
                    // Delegate to SubCommands
                    else
                    {
                        boolean foundChildCommand = false;
                        for (InternalCommand internalCommand : childCommands)
                        {
                            if (arguments.peek().equalsIgnoreCase(internalCommand.name))
                            {
                                // Remove the current command arg
                                arguments.remove();
                                internalCommand.invokeCommand(sender, arguments);
                                foundChildCommand = true;
                                break;
                            }
                        }

                        // No fitting child commands were found.
                        if (!foundChildCommand)
                        {
                            this.executeIfAllowed(sender, arguments);
                        }
                    }
                }
            }
            else
            {
                // Normal command procedure
                executeIfAllowed(sender, arguments);
            }
        }
        else
        {
            sender.sendMessage(prefix + ChatColor.RED + "You don't have permission to do this.");
        }
    }

    private void executeIfAllowed(CommandSender sender, Queue<String> arguments)
    {
        if (arguments.size() >= minArguments && arguments.size() <= maxArguments)
        {
            if (!onlyPlayers || sender instanceof Player)
            {
                execute(sender, arguments);
            }
            else
            {
                sender.sendMessage(prefix + ChatColor.RED + "Only a player can use this command.");
            }
        }
        else
        {
            sender.sendMessage(prefix + ChatColor.RED + "Wrong amount of arguments: " + arguments.size() + " expected: " + minArguments + " to " + maxArguments);
        }
    }

    protected abstract void execute(CommandSender sender, Queue<String> arguments);

    protected abstract String[] getCommandHelp();

    protected abstract Set<InternalCommand> getChildCommands();

    protected abstract String[] getTabPossibilities();

    protected String[] getChildTabs()
    {
        final Collection<InternalCommand> childs = this.getChildCommands();
        final String[] tabs = new String[childs.size()];

        int index = 0;
        for (InternalCommand child : childs)
        {
            tabs[index++] = child.name;
        }

        return tabs;
    }

    protected String[] getPlayerNameTabs()
    {
        final Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        final String[] tab = new String[onlinePlayers.size()];

        int index = 0;
        for (Player player : onlinePlayers)
        {
            tab[index++] = player.getName();
        }
        return tab;
    }
}

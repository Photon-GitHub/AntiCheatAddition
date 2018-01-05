package de.photon.AACAdditionPro.command;

import de.photon.AACAdditionPro.InternalPermission;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public abstract class InternalCommand
{
    protected static final String PREFIX = ChatColor.DARK_RED + "[AACAdditionPro] ";
    protected static final String PLAYER_NOT_FOUND_MESSAGE = PREFIX + ChatColor.RED + "Player could not be found.";

    public final String name;
    private final InternalPermission permission;
    private final boolean onlyPlayers;
    private final byte minArguments;
    private final byte maxArguments;

    @Getter
    private final Set<InternalCommand> childCommands;

    public InternalCommand(String name, byte minArguments, InternalCommand... childCommands)
    {
        this(name, null, minArguments, childCommands);
    }

    public InternalCommand(String name, InternalPermission permission, byte minArguments, InternalCommand... childCommands)
    {
        this(name, permission, false, minArguments, childCommands);
    }

    public InternalCommand(String name, InternalPermission permission, boolean onlyPlayers, byte minArguments, InternalCommand... childCommands)
    {
        this(name, permission, onlyPlayers, minArguments, Byte.MAX_VALUE, childCommands);
    }

    public InternalCommand(String name, InternalPermission permission, boolean onlyPlayers, byte minArguments, byte maxArguments, InternalCommand... childCommands)
    {
        this.name = name;
        this.permission = permission;
        this.onlyPlayers = onlyPlayers;
        this.minArguments = minArguments;
        this.maxArguments = maxArguments;

        if (childCommands.length > 1)
        {
            this.childCommands = new HashSet<>(Arrays.asList(childCommands));
        }
        else
        {
            this.childCommands = null;
        }
    }

    void invokeCommand(CommandSender sender, Queue<String> arguments)
    {
        // No permission is set or the sender has the permission
        if (InternalPermission.hasPermission(sender, this.permission))
        {
            if (arguments.size() > 0)
            {
                if (arguments.peek().equals("?"))
                {
                    for (final String help : this.getCommandHelp())
                    {
                        sender.sendMessage(PREFIX + ChatColor.GOLD + help);
                    }
                    return;
                }

                final Set<InternalCommand> childCommands = this.getChildCommands();

                if (childCommands != null)
                {
                    // Delegate to SubCommands
                    for (final InternalCommand internalCommand : childCommands)
                    {
                        if (arguments.peek().equalsIgnoreCase(internalCommand.name))
                        {
                            // Remove the current command arg
                            arguments.remove();
                            internalCommand.invokeCommand(sender, arguments);
                            return;
                        }
                    }
                }
            }

            // Normal command procedure or childCommands is null or no fitting child commands were found.
            this.executeIfAllowed(sender, arguments);
        }
        else
        {
            sender.sendMessage(PREFIX + ChatColor.RED + "You don't have permission to do this.");
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
                sender.sendMessage(PREFIX + ChatColor.RED + "Only a player can use this command.");
            }
        }
        else
        {
            sender.sendMessage(PREFIX + ChatColor.RED + "Wrong amount of arguments: " + arguments.size() + " expected: " + minArguments + " to " + maxArguments);
        }
    }

    protected abstract void execute(CommandSender sender, Queue<String> arguments);

    protected abstract String[] getCommandHelp();

    protected abstract String[] getTabPossibilities();

    protected String[] getChildTabs()
    {
        final Collection<InternalCommand> children = this.getChildCommands();
        final String[] tabs = new String[children.size()];

        int index = 0;
        for (InternalCommand child : children)
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

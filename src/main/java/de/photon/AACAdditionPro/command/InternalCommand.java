package de.photon.AACAdditionPro.command;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.InternalPermission;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

        this.childCommands = childCommands.length != 0 ? ImmutableSet.copyOf(childCommands) : Collections.EMPTY_SET;
    }

    void invokeCommand(CommandSender sender, Queue<String> arguments)
    {
        // No permission is set or the sender has the permission
        if (InternalPermission.hasPermission(sender, this.permission))
        {
            if (arguments.peek() != null)
            {
                if (arguments.peek().equals("?"))
                {
                    for (final String help : this.getCommandHelp())
                    {
                        sender.sendMessage(PREFIX + ChatColor.GOLD + help);
                    }
                    return;
                }

                // Delegate to SubCommands
                for (final InternalCommand internalCommand : this.childCommands)
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

    protected abstract List<String> getTabPossibilities();

    /**
     * Returns an array of {@link String}s containing the names of all child commands.
     */
    protected List<String> getChildTabs()
    {
        final List<String> childTabs = new ArrayList<>(this.childCommands.size());
        for (InternalCommand internalCommand : this.childCommands)
        {
            childTabs.add(internalCommand.name);
        }
        return childTabs;
    }

    /**
     * Returns an array of {@link String}s containing the names of all currently online players.
     */
    protected List<String> getPlayerNameTabs()
    {
        final List<String> playerNameList = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers())
        {
            playerNameList.add(player.getName());
        }
        return playerNameList;
    }
}

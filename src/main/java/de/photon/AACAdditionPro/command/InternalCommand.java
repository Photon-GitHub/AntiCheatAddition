package de.photon.AACAdditionPro.command;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.InternalPermission;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class InternalCommand
{
    protected static final String PREFIX = ChatColor.DARK_RED + "[AACAdditionPro] ";

    public final String name;
    private final InternalPermission permission;
    private final boolean onlyPlayers;
    private final byte minArguments;
    private final byte maxArguments;

    @Getter
    private final Set<InternalCommand> childCommands;

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
        if (!InternalPermission.hasPermission(sender, this.permission))
        {
            sender.sendMessage(PREFIX + ChatColor.RED + "You don't have permission to do this.");
            return;
        }

        // Any additional arguments
        if (arguments.peek() != null)
        {
            // Command help
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

        // ------- Normal command procedure or childCommands is null or no fitting child commands were found. ------- //

        // Correct amount of arguments
        if (arguments.size() < minArguments || arguments.size() > maxArguments)
        {
            sender.sendMessage(PREFIX + ChatColor.RED + "Wrong amount of arguments: " + arguments.size() + " expected: " + minArguments + " to " + maxArguments);
            return;
        }

        // Only players
        if (onlyPlayers && !(sender instanceof Player))
        {
            sender.sendMessage(PREFIX + ChatColor.RED + "Only a player can use this command.");
            return;
        }

        execute(sender, arguments);
    }

    protected abstract void execute(CommandSender sender, Queue<String> arguments);

    protected abstract String[] getCommandHelp();

    protected abstract List<String> getTabPossibilities();

    /**
     * Returns an array of {@link String}s containing the names of all child commands.
     */
    protected List<String> getChildTabs()
    {
        return this.childCommands.stream().map(internalCommand -> internalCommand.name).collect(Collectors.toList());
    }

    /**
     * Returns an array of {@link String}s containing the names of all currently online players.
     */
    protected List<String> getPlayerNameTabs()
    {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    /**
     * Checks a numeric argument for correctness.
     *
     * @param sender   the sender of the command (necessary for error messsages)
     * @param argument the {@link String} which should be the desired number
     * @param min      the lower boundary of the number
     * @param max      the upper boundary of the number
     *
     * @return null if the argument is not a number, too big or too small and the number if everything is fine.
     */
    public static Double validateNumberArgument(CommandSender sender, String argument, double min, double max)
    {
        final double number;
        try
        {
            // Use .parseDouble instead of .valueOf for better performance because of redundant boxing.
            number = Double.parseDouble(argument);
        } catch (NumberFormatException exception)
        {
            sender.sendMessage(PREFIX + ChatColor.RED + "Please enter a valid number.");
            return null;
        }

        if (number > max)
        {
            sender.sendMessage(PREFIX + ChatColor.RED + "The number must at most be " + max);
            return null;
        }

        if (number < min)
        {
            sender.sendMessage(PREFIX + ChatColor.RED + "The number must at least be " + min);
            return null;
        }

        return number;
    }

    /**
     * Sends a message showing that a "Player could not be found."
     */
    public static void sendPlayerNotFoundMessage(CommandSender sender)
    {
        sender.sendMessage(PREFIX + ChatColor.RED + "Player could not be found.");
    }
}

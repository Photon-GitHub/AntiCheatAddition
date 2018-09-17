package de.photon.AACAdditionPro.command;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import de.photon.AACAdditionPro.InternalPermission;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

public abstract class InternalCommand
{
    protected static final String PREFIX = ChatColor.DARK_RED + "[AACAdditionPro] ";

    private final String name;
    private final InternalPermission permission;
    private final byte minArguments;
    private final byte maxArguments;

    private final Map<String, InternalCommand> childCommands;
    protected final List<String> childTabs;

    public InternalCommand(String name, InternalPermission permission, InternalCommand... childCommands)
    {
        this(name, permission, (byte) 0, childCommands);
    }

    public InternalCommand(String name, InternalPermission permission, byte minArguments, InternalCommand... childCommands)
    {
        this(name, permission, minArguments, Byte.MAX_VALUE, childCommands);
    }

    public InternalCommand(String name, InternalPermission permission, byte minArguments, byte maxArguments, InternalCommand... childCommands)
    {
        this.name = name;
        this.permission = permission;
        this.minArguments = minArguments;
        this.maxArguments = maxArguments;

        final ImmutableList.Builder<String> listBuilder = ImmutableList.builder();
        final ImmutableMap.Builder<String, InternalCommand> builder = ImmutableMap.builder();

        for (InternalCommand childCommand : childCommands)
        {
            listBuilder.add(childCommand.name);
            builder.put(childCommand.name, childCommand);
        }

        this.childTabs = listBuilder.build();
        this.childCommands = builder.build();
    }

    /**
     * Handle a command with certain arguments.
     *
     * @param sender    the {@link CommandSender} that originally sent the command.
     * @param arguments a {@link Queue} which contains the remaining arguments.
     */
    void invokeCommand(final CommandSender sender, final Queue<String> arguments)
    {
        // No permission is set or the sender has the permission
        if (!InternalPermission.hasPermission(sender, this.permission))
        {
            sendNoPermissionMessage(sender);
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
            final InternalCommand childCommand = this.getChildCommandByNameIgnoreCase(arguments.peek());

            if (childCommand != null)
            {
                // Remove the current command arg
                arguments.remove();
                childCommand.invokeCommand(sender, arguments);
                return;
            }
        }

        // ------- Normal command procedure or childCommands is null or no fitting child commands were found. ------- //

        // Correct amount of arguments
        if (!MathUtils.inRange(minArguments, maxArguments, arguments.size()))
        {
            sendErrorMessage(sender, "Wrong amount of arguments: " + arguments.size() + " expected: " + minArguments + " to " + maxArguments);
            return;
        }

        execute(sender, arguments);
    }

    /**
     * This contains the code that is actually executed if everything is correct.
     */
    protected abstract void execute(CommandSender sender, Queue<String> arguments);

    /**
     * @return an array of {@link String}s in which a single {@link String} represents a line in the shown command help.
     */
    protected abstract String[] getCommandHelp();

    protected List<String> getTabPossibilities()
    {
        return this.childTabs;
    }

    /**
     * Gets a child command of this command by its name.
     *
     * @param name the name of the supposed child command.
     *
     * @return the child command or <code>null</code> if no child command has that name.
     */
    protected InternalCommand getChildCommandByNameIgnoreCase(final String name)
    {
        return this.childCommands.get(name.toLowerCase());
    }

    /**
     * Returns an array of {@link String}s containing the names of all currently online players.
     */
    protected static List<String> getPlayerNameTabs()
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
            sendErrorMessage(sender, "Please enter a valid number.");
            return null;
        }

        if (!MathUtils.inRange(min, max, number))
        {
            sendErrorMessage(sender, "The number " + number + " must be between " + min + " and " + max);
            return null;
        }

        return number;
    }

    /**
     * Sends a message showing that a "Player could not be found."
     */
    public static void sendNoPermissionMessage(final CommandSender sender)
    {
        sendErrorMessage(sender, "You don't have permission to do this.");
    }

    /**
     * Sends a message showing that a "Player could not be found."
     */
    public static void sendPlayerNotFoundMessage(final CommandSender sender)
    {
        sendErrorMessage(sender, "Player could not be found.");
    }

    /**
     * Shortcut for sender.sendMessage(PREFIX + ChatColor.RED + message);
     */
    public static void sendErrorMessage(final CommandSender sender, final String message)
    {
        sender.sendMessage(PREFIX + ChatColor.RED + message);
    }
}

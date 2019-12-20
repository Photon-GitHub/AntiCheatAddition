package de.photon.aacadditionpro.command;

import com.google.common.collect.ImmutableMap;
import de.photon.aacadditionpro.InternalPermission;
import de.photon.aacadditionpro.util.mathematics.MathUtils;
import de.photon.aacadditionpro.util.messaging.ChatMessage;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public abstract class InternalCommand
{
    protected final String name;
    private final InternalPermission permission;
    private final int minArguments;
    private final int maxArguments;

    private final List<String> commandHelp;
    final Map<String, InternalCommand> childCommands;
    @Getter
    private final TabCompleteSupplier tabCompleteSupplier;

    public InternalCommand(String name, InternalPermission permission, List<String> commandHelp, Set<InternalCommand> childCommands, TabCompleteSupplier.Builder tabCompleteSupplier)
    {
        this(name, permission, (byte) 0, commandHelp, childCommands, tabCompleteSupplier);
    }

    public InternalCommand(String name, InternalPermission permission, int minArguments, List<String> commandHelp, Set<InternalCommand> childCommands, TabCompleteSupplier.Builder tabCompleteSupplier)
    {
        this(name, permission, minArguments, Byte.MAX_VALUE, commandHelp, childCommands, tabCompleteSupplier);
    }

    public InternalCommand(String name, InternalPermission permission, int minArguments, int maxArguments, List<String> commandHelp, Set<InternalCommand> childCommands, TabCompleteSupplier.Builder tabCompleteSupplier)
    {
        this.name = name;
        this.permission = permission;
        this.minArguments = minArguments;
        this.maxArguments = maxArguments;
        this.commandHelp = commandHelp;

        final ImmutableMap.Builder<String, InternalCommand> builder = ImmutableMap.builder();

        for (InternalCommand childCommand : childCommands) {
            builder.put(childCommand.name, childCommand);
        }

        this.childCommands = builder.build();
        // Add the child commands.
        this.tabCompleteSupplier = tabCompleteSupplier.constants(this.childCommands.keySet()).build();
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
        if (!this.permission.hasPermission(sender)) {
            ChatMessage.sendNoPermissionMessage(sender);
            return;
        }

        final String peek = arguments.peek();

        if (peek != null) {
            // Command help
            if ("?".equals(peek)) {
                sendCommandHelp(sender);
                return;
            }

            final InternalCommand childCommand = this.childCommands.get(peek);
            if (childCommand != null) {
                // Remove the current command arg
                arguments.remove();
                childCommand.invokeCommand(sender, arguments);
                return;
            }
        }

        // ------- Normal command procedure or childCommands is null or no fitting child commands were found. ------- //

        // Correct amount of arguments
        if (!MathUtils.inRange(minArguments, maxArguments, arguments.size())) {
            ChatMessage.sendErrorMessage(sender, "Wrong amount of arguments: " + arguments.size() + " expected: " + minArguments + " to " + maxArguments);
            return;
        }

        execute(sender, arguments);
    }

    /**
     * This contains the code that is actually executed if everything is correct.
     */
    protected abstract void execute(CommandSender sender, Queue<String> arguments);

    private void sendCommandHelp(CommandSender sender)
    {
        for (final String help : this.commandHelp) {
            ChatMessage.sendInfoMessage(sender, ChatColor.GOLD, help);
        }
    }
}

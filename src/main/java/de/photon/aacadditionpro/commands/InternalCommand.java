package de.photon.aacadditionpro.commands;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.util.messaging.ChatMessage;
import lombok.Getter;
import org.bukkit.command.CommandSender;

import java.util.Locale;
import java.util.Queue;

public abstract class InternalCommand
{
    protected final String name;
    private final CommandAttributes commandAttributes;
    @Getter
    private final TabCompleteSupplier tabCompleteSupplier;

    public InternalCommand(String name, CommandAttributes commandAttributes, TabCompleteSupplier.Builder tabCompleteSupplier)
    {
        Preconditions.checkNotNull(name, "Tried to create command with null name.");
        Preconditions.checkNotNull(commandAttributes, "Tried to create command with null attributes.");
        Preconditions.checkNotNull(tabCompleteSupplier, "Tried to create command with null tab supplier.");
        Preconditions.checkArgument(name.equals(name.toLowerCase(Locale.ENGLISH)), "Tried to create command with upper case letters in name.");

        this.name = name;
        this.commandAttributes = commandAttributes;
        this.tabCompleteSupplier = tabCompleteSupplier.childCommands(commandAttributes).build();
    }

    /**
     * Gets the child command of the given name or null if no such command exists.
     */
    public InternalCommand getChildCommand(String name)
    {
        return this.commandAttributes.getChildCommands().get(name);
    }

    /**
     * Handle a command with certain arguments.
     *
     * @param sender    the {@link CommandSender} that originally sent the command.
     * @param arguments a {@link Queue} which contains the remaining arguments.
     */
    void invokeCommand(final CommandSender sender, final Queue<String> arguments)
    {
        if (!this.commandAttributes.hasPermission(sender)) return;
        if (!this.commandAttributes.argumentsInRange(arguments.size())) return;

        if (!arguments.isEmpty()) {
            final String nextArgument = arguments.peek();

            if ("help".equals(nextArgument)) {
                this.commandAttributes.sendCommandHelp(sender);
                return;
            }

            final InternalCommand childCommand = this.getChildCommand(nextArgument);
            if (childCommand != null) {
                // Remove the current command arg
                arguments.remove();
                childCommand.invokeCommand(sender, arguments);
                return;
            }
        }
        // ------- Normal command procedure or childCommands is null or no fitting child commands were found. ------- //

        // Correct amount of arguments
        if (!this.commandAttributes.argumentsInRange(arguments.size())) {
            ChatMessage.sendMessage(sender, "Wrong amount of arguments: " + arguments.size() + " expected: " + this.commandAttributes.getMinArguments() + " to " + this.commandAttributes.getMaxArguments());
            ChatMessage.sendMessage(sender, "For further information use /<command> help");
            return;
        }

        execute(sender, arguments);
    }

    /**
     * This contains the code that is actually executed if everything is correct.
     */
    protected abstract void execute(CommandSender sender, Queue<String> arguments);
}

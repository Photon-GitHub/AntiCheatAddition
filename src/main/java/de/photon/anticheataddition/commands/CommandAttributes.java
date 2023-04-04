package de.photon.anticheataddition.commands;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSortedMap;
import de.photon.anticheataddition.InternalPermission;
import de.photon.anticheataddition.util.messaging.ChatMessage;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public final class CommandAttributes
{
    @Getter @NotNull private final SortedMap<String, InternalCommand> childCommands;
    @NotNull private final List<String> commandHelp;
    @NotNull private final InternalPermission permission;
    private final int minArguments;
    private final int maxArguments;

    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Checks if the given argument count is valid by the attributes.
     *
     * @param arguments            the argument count.
     * @param errorMessageReceiver the receiver of any error message if the arguments are out of range.
     *
     * @return <code>true</code> iff minArguments <= arguments <= maxArguments
     */
    public boolean argumentsOutOfRange(int arguments, CommandSender errorMessageReceiver)
    {
        if (arguments < minArguments || arguments > maxArguments) {
            ChatMessage.sendMessage(errorMessageReceiver, "Wrong amount of arguments: " + arguments + ". Expected: " + minArguments + " to " + maxArguments);
            ChatMessage.sendMessage(errorMessageReceiver, "For further information use /<command> help");
            return true;
        }
        return false;
    }

    public boolean hasCommandHelp()
    {
        return !commandHelp.isEmpty();
    }

    public boolean hasPermission(Permissible permissible)
    {
        // This will automatically return true for null-permissions.
        return this.permission.hasPermission(permissible);
    }

    /**
     * Sends the complete command help to a player.
     */
    public void sendCommandHelp(CommandSender sender)
    {
        for (String line : this.commandHelp) ChatMessage.sendMessage(sender, line);
    }

    /**
     * Factory for building a {@link CommandAttributes} instance.
     */
    public static class Builder
    {
        private final ImmutableSortedMap.Builder<String, InternalCommand> childCommandsBuilder = ImmutableSortedMap.naturalOrder();
        private final List<String> commandHelp = new ArrayList<>();
        private InternalPermission permission = InternalPermission.NONE;
        private int minArguments = 0;
        private int maxArguments = 25;

        /**
         * The minimum arguments of the command that should be enforced.
         */
        public Builder minArguments(int minArguments)
        {
            Preconditions.checkArgument(minArguments >= 0);
            this.minArguments = minArguments;
            return this;
        }

        /**
         * The maximum arguments of the command that should be enforced.
         */
        public Builder maxArguments(int maxArguments)
        {
            Preconditions.checkArgument(maxArguments >= 0);
            this.maxArguments = maxArguments;
            return this;
        }

        /**
         * Shortcut for setting both the minimum and maximum arguments to the same value.
         */
        public Builder exactArguments(int exactArg)
        {
            return minArguments(exactArg).maxArguments(exactArg);
        }

        /**
         * Sets the permission of the command.
         */
        public Builder setPermission(InternalPermission permission)
        {
            this.permission = permission;
            return this;
        }

        /**
         * Directly sets the command help.
         */
        public Builder addCommandHelp(String... commandHelp)
        {
            Collections.addAll(this.commandHelp, commandHelp);
            return this;
        }

        /**
         * Directly sets the command help.
         */
        public Builder addChildCommands(final InternalCommand... commands)
        {
            for (InternalCommand command : commands) addChildCommand(command);
            return this;
        }

        /**
         * Add a single line to the command help.
         */
        public Builder addChildCommand(final InternalCommand command)
        {
            this.childCommandsBuilder.put(command.getName(), command);
            return this;
        }

        public CommandAttributes build()
        {
            final var childCommands = this.childCommandsBuilder.build();
            if (!childCommands.isEmpty()) this.addCommandHelp("Subcommands of this command are: " + String.join(", ", childCommands.keySet()));

            return new CommandAttributes(childCommands, List.copyOf(commandHelp), permission, minArguments, maxArguments);
        }
    }
}

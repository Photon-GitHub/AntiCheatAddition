package de.photon.anticheataddition.commands;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.messaging.ChatMessage;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Queue;


@Getter
@EqualsAndHashCode(doNotUseGetters = true, cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public abstract class InternalCommand
{
    private static final String NO_PERMISSION_MESSAGE = AntiCheatAddition.getInstance().getConfig().getString("CommandHider.permissions_message", "Unknown command. Please type /help for help.");

    /**
     * The name of the command. Guaranteed to be lowercase only.
     */
    @NotNull private final String name;
    @NotNull private final CommandAttributes commandAttributes;
    @EqualsAndHashCode.Exclude @NotNull private final TabCompleteSupplier tabCompleteSupplier;

    protected InternalCommand(@NotNull String name, @NotNull CommandAttributes commandAttributes, @NotNull TabCompleteSupplier.Builder tabCompleteSupplier)
    {
        Preconditions.checkNotNull(name, "Tried to create command with null name.");
        Preconditions.checkNotNull(commandAttributes, "Tried to create command with null attributes.");
        Preconditions.checkNotNull(tabCompleteSupplier, "Tried to create command with null tab supplier.");
        Preconditions.checkArgument(name.equals(name.toLowerCase(Locale.ENGLISH)), "Tried to create command with upper case letters in name.");

        this.name = name;
        this.commandAttributes = commandAttributes;
        this.tabCompleteSupplier = tabCompleteSupplier.build(commandAttributes);
    }

    /**
     * Gets a {@link Player} from their name and sends a not found message if the {@link Player} could not be found.
     */
    @Nullable
    protected static Player parsePlayer(CommandSender sender, String nameOfPlayer)
    {
        if (nameOfPlayer == null) {
            ChatMessage.sendMessage(sender, "Please specify a player.");
            return null;
        }

        final var player = Bukkit.getServer().getPlayer(nameOfPlayer);
        if (player == null) ChatMessage.sendMessage(sender, "The specified player could not be found.");
        return player;
    }

    /**
     * Gets a {@link User} from their name and sends a not found message if the {@link User} could not be found.
     */
    @Nullable
    protected static User parseUser(CommandSender sender, String nameOfUser)
    {
        final var player = parsePlayer(sender, nameOfUser);
        if (player == null) return null;
        final var user = User.getUser(player);
        if (user == null) ChatMessage.sendMessage(sender, "The specified user could not be found.");
        return user;
    }

    /**
     * This checks if an object is null. If it is, it will send the message to the recipient, else nothing will happen.
     *
     * @return <code>true</code> when the object is null, else <code>false</code>.
     */
    @Contract("null, _ , _ -> true; !null, _ , _ -> false")
    protected static boolean checkNotNullElseSend(final Object notNull, final CommandSender recipient, final String message)
    {
        final boolean n = (notNull == null);
        if (n) ChatMessage.sendMessage(recipient, message);
        return n;
    }

    /**
     * Tries to parse an integer. If the integer could not be parsed, send the message to the recipient.
     *
     * @return null if the string was not a valid integer, else the parsed value
     */
    protected static Integer parseIntElseSend(final CommandSender recipient, final String toParse)
    {
        try {
            return Integer.parseInt(toParse);
        } catch (NumberFormatException e) {
            ChatMessage.sendMessage(recipient, "Please specify a valid integer.");
        }
        return null;
    }

    /**
     * Tries to parse a long. If the long could not be parsed, send the message to the recipient.
     *
     * @return null if the string was not a valid long, else the parsed value
     */
    protected static Long parseLongElseSend(final CommandSender recipient, final String toParse)
    {
        try {
            return Long.parseLong(toParse);
        } catch (NumberFormatException e) {
            ChatMessage.sendMessage(recipient, "Please specify a valid integer.");
        }
        return null;
    }

    /**
     * Gets the child command of the given name or null if no such command exists.
     */
    public InternalCommand getChildCommand(@NotNull final String name)
    {
        return this.commandAttributes.getChildCommands().get(name);
    }

    /**
     * Handle a command with certain arguments.
     *
     * @param sender    the {@link CommandSender} that originally sent the command.
     * @param arguments a {@link Queue} which contains the remaining arguments.
     */
    protected void invokeCommand(@NotNull final CommandSender sender, @NotNull final Queue<String> arguments)
    {
        if (!this.commandAttributes.hasPermission(sender)) {
            sender.sendMessage(NO_PERMISSION_MESSAGE);
            return;
        }

        if (!arguments.isEmpty()) {
            final String nextArgument = arguments.peek();

            if ("help".equals(nextArgument)) {
                this.commandAttributes.sendCommandHelp(sender);
                return;
            }

            final var childCommand = this.getChildCommand(nextArgument);
            if (childCommand != null) {
                // Remove the current command arg
                arguments.remove();
                childCommand.invokeCommand(sender, arguments);
                return;
            }
        }
        // ------- Normal command procedure or childCommands is null or no fitting child commands were found. ------- //

        // Correct amount of arguments
        if (this.commandAttributes.argumentsOutOfRange(arguments.size(), sender)) return;

        execute(sender, arguments);
    }

    /**
     * This contains the code that is actually executed if everything is correct.
     */
    protected abstract void execute(CommandSender sender, Queue<String> arguments);
}

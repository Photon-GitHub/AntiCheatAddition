package de.photon.anticheataddition.util.messaging;

import de.photon.anticheataddition.AntiCheatAddition;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

@UtilityClass
public final class ChatMessage
{
    /**
     * Sends a message with the AntiCheatAddition prefix to a single recipient.
     */
    public static void sendMessage(CommandSender recipient, String message)
    {
        recipient.sendMessage(AntiCheatAddition.ANTICHEAT_ADDITION_PREFIX + message);
    }

    /**
     * Sends a message with the AntiCheatAddition prefix to a single recipient.
     * This method should be called asynchronously, else use {@link #sendMessage(CommandSender, String)}
     */
    public static void sendSyncMessage(CommandSender recipient, String message)
    {
        Bukkit.getScheduler().runTask(AntiCheatAddition.getInstance(), () -> ChatMessage.sendMessage(recipient, message));
    }

    /**
     * Sends a message with the AntiCheatAddition prefix to multiple recipients, caching the message for less String
     * concatenations.
     */
    public static void sendMessage(final Iterable<? extends CommandSender> senders, final String message)
    {
        final var prefixedMessage = AntiCheatAddition.ANTICHEAT_ADDITION_PREFIX + message;
        for (CommandSender cs : senders) cs.sendMessage(prefixedMessage);
    }

    /**
     * Sends a message with the AntiCheatAddition prefix to multiple recipients, caching the message for less String
     * concatenations.
     * This method should be called asynchronously, else use {@link #sendMessage(Iterable, String)}
     */
    public static void sendSyncMessage(final Iterable<? extends CommandSender> senders, final String message)
    {
        Bukkit.getScheduler().runTask(AntiCheatAddition.getInstance(), () -> ChatMessage.sendMessage(senders, message));
    }
}

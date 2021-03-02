package de.photon.aacadditionpro.util.messaging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatMessage
{
    public static final String AACADDITIONPRO_PREFIX = ChatColor.AQUA + "[AACAdditionPro] " + ChatColor.GRAY;

    /**
     * Sends a message with the AACAdditionPro prefix to a single recipient.
     */
    public static void sendMessage(CommandSender recipient, String message)
    {
        recipient.sendMessage(AACADDITIONPRO_PREFIX + message);
    }

    /**
     * Sends a message with the AACAdditionPro prefix to multiple recipients, caching the message for less String
     * concatenations.
     */
    public static void sendMessage(final Iterable<CommandSender> sender, final String message)
    {
        final String prefixedMessage = AACADDITIONPRO_PREFIX + message;
        for (CommandSender cs : sender) cs.sendMessage(prefixedMessage);
    }

    /**
     * Sends the "You don't have permission to do that." message with prefix to a recipient.
     */
    public static void sendNoPermissionMessage(CommandSender recipient)
    {
        sendMessage(recipient, "You don't have permission to do that.");
    }

    /**
     * Sends the "The specified player could not be found." message with prefix to a recipient.
     */
    public static void sendPlayerNotFoundMessage(CommandSender recipient)
    {
        sendMessage(recipient, "The specified player could not be found.");
    }
}

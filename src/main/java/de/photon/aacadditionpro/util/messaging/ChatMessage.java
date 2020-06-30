package de.photon.aacadditionpro.util.messaging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatMessage
{
    public static final String AACADDITIONPRO_PREFIX = ChatColor.DARK_RED + "[AACAdditionPro] ";
    private static final String ERROR_PREFIX = AACADDITIONPRO_PREFIX + ChatColor.RED;

    public static void sendInfoMessage(CommandSender sender, ChatColor color, String message)
    {
        sender.sendMessage(AACADDITIONPRO_PREFIX + color + message);
    }

    public static void sendErrorMessage(CommandSender sender, String message)
    {
        sender.sendMessage(ERROR_PREFIX + message);
    }

    public static void sendNoPermissionMessage(CommandSender sender)
    {
        sendErrorMessage(sender, "You don't have permission to do that.");
    }

    public static void sendPlayerNotFoundMessage(CommandSender sender)
    {
        sendErrorMessage(sender, "The specified player could not be found.");
    }
}

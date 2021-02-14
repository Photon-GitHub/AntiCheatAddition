package de.photon.aacadditionpro.util.messaging;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatMessage
{
    public static final String AACADDITIONPRO_PREFIX = ChatColor.AQUA + "[AACAdditionPro] " + ChatColor.GRAY;

    public static void sendMessage(CommandSender sender, String message)
    {
        sender.sendMessage(AACADDITIONPRO_PREFIX + message);
    }


    public static void sendNoPermissionMessage(CommandSender sender)
    {
        sendMessage(sender, "You don't have permission to do that.");
    }

    public static void sendPlayerNotFoundMessage(CommandSender sender)
    {
        sendMessage(sender, "The specified player could not be found.");
    }
}

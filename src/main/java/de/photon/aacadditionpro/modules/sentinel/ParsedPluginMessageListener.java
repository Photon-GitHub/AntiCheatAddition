package de.photon.aacadditionpro.modules.sentinel;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;

public interface ParsedPluginMessageListener extends PluginMessageListener
{
    @Override
    default void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message)
    {
        String clientData;
        try {
            clientData = new String(message, StandardCharsets.UTF_8);
        } catch (Exception e) {
            clientData = "";
        }

        onPluginMessageReceived(channel, player, clientData);
    }

    void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull String message);
}

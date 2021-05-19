package de.photon.aacadditionpro.util.pluginmessage;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public interface MessageChannel
{
    MessageChannel MC_BRAND_CHANNEL = MessageChannel.of("minecraft", "brand", "MC|Brand");

    static MessageChannel of(final String channel)
    {
        val splitNew = channel.split(":");
        val splitOld = channel.split("\\|");
        if (splitNew.length == 2) return MessageChannel.of(splitNew[0], splitNew[1]);
        if (splitOld.length == 2) return MessageChannel.of(splitOld[0].toLowerCase(Locale.ENGLISH), splitNew[1].toLowerCase(Locale.ENGLISH), channel);
        throw new IllegalArgumentException("Cannot recognize " + channel + " as a channel. Please make sure that it contains either the ':' or the '|' char.");
    }

    static MessageChannel of(final String prefix, final String key)
    {
        final String legacyName;
        val upperStartTempKey = Character.toUpperCase(key.charAt(0)) + key.substring(1);
        switch (prefix) {
            case "minecraft":
                legacyName = "MC|" + upperStartTempKey;
                break;
            case "px":
                legacyName = "PX|" + upperStartTempKey;
                break;
            case "wdl":
                legacyName = "WDL|" + key.toUpperCase();
                break;
            default:
                legacyName = null;
                break;
        }
        return MessageChannel.of(prefix, key, legacyName);
    }

    static MessageChannel of(final String prefix, final String key, final String legacyName)
    {
        if (ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS.contains(ServerVersion.getActiveServerVersion())) {
            return legacyName == null ? EmptyMessageChannel.EMPTY : new LegacyMessageChannel(legacyName);
        } else {
            return prefix == null || key == null ? EmptyMessageChannel.EMPTY : new KeyMessageChannel(prefix, key);
        }
    }

    static MessageChannel ofLegacy(final String legacyName)
    {
        return legacyName != null && ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS.contains(ServerVersion.getActiveServerVersion()) ? new LegacyMessageChannel(legacyName) : EmptyMessageChannel.EMPTY;
    }

    /**
     * Gets the channel for the current {@link ServerVersion} or null if it doesn't support the current {@link ServerVersion}
     */
    @NotNull
    String getChannel();

    /**
     * Registers the incoming channel for a certain {@link PluginMessageListener}
     */
    default void registerIncomingChannel(final PluginMessageListener listener)
    {
        Bukkit.getMessenger().registerIncomingPluginChannel(AACAdditionPro.getInstance(), this.getChannel(), listener);
    }

    /**
     * Unregisters the incoming channel for a certain {@link PluginMessageListener}
     */
    default void unregisterIncomingChannel(final PluginMessageListener listener)
    {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(AACAdditionPro.getInstance(), this.getChannel(), listener);
    }

    /**
     * Registers the outgoing channel for a certain {@link PluginMessageListener}
     */
    default void registerOutgoingChannel()
    {
        Bukkit.getMessenger().registerOutgoingPluginChannel(AACAdditionPro.getInstance(), this.getChannel());
    }

    /**
     * Unregisters the outgoing channel for a certain {@link PluginMessageListener}
     */
    default void unregisterOutgoingChannel()
    {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(AACAdditionPro.getInstance(), this.getChannel());
    }
}

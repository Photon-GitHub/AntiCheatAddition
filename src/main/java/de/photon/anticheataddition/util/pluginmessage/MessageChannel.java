package de.photon.anticheataddition.util.pluginmessage;

import com.comphenix.protocol.wrappers.MinecraftKey;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public interface MessageChannel
{
    MessageChannel EMPTY = Optional::empty;

    MessageChannel MC_BRAND_CHANNEL = MessageChannel.of("minecraft", "brand", "MC|Brand");
    MessageChannel LABYMOD_CHANNEL = MessageChannel.of("labymod3", "main", "LMC");
    MessageChannel SCHEMATICA_CHANNEL = MessageChannel.ofLegacy("schematica");
    MessageChannel BETTER_SPRINTING_CHANNEL = MessageChannel.of("bsm", "settings", "BSM");

    @NotNull
    static MessageChannel of(final MinecraftKey channel)
    {
        return MessageChannel.of(channel.getPrefix(), channel.getKey());
    }

    @NotNull
    static MessageChannel of(final String channel)
    {
        val splitNew = channel.split(":");
        return splitNew.length == 2 ? MessageChannel.of(splitNew[0], splitNew[1]) : ofLegacy(channel);
    }

    @NotNull
    static MessageChannel of(final String prefix, final String key)
    {
        final String upperStartTempKey = StringUtils.capitalize(key);
        final String legacyName = switch (prefix) {
            case "minecraft" -> "MC|" + upperStartTempKey;
            case "px" -> "PX|" + upperStartTempKey;
            case "wdl" -> "WDL|" + key.toUpperCase();
            default -> prefix + ':' + key;
        };
        return MessageChannel.of(prefix, key, legacyName);
    }

    @NotNull
    static MessageChannel of(final String prefix, final String key, final String legacyName)
    {
        if (ServerVersion.containsActive(ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS)) return ofLegacy(legacyName);
        else return prefix == null || key == null ? MessageChannel.EMPTY : new KeyMessageChannel(prefix, key);
    }

    @NotNull
    static MessageChannel ofLegacy(final String legacyName)
    {
        return legacyName != null && ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS.contains(ServerVersion.ACTIVE) ? new LegacyMessageChannel(legacyName) : MessageChannel.EMPTY;
    }

    /**
     * Gets the channel for the current {@link ServerVersion} or an empty {@link String} if it doesn't support the current {@link ServerVersion}
     */
    Optional<String> getChannel();

    /**
     * Registers the incoming channel for a certain {@link PluginMessageListener}
     */
    default void registerIncomingChannel(final PluginMessageListener listener)
    {
        this.getChannel().ifPresent(channel -> Bukkit.getMessenger().registerIncomingPluginChannel(AntiCheatAddition.getInstance(), channel, listener));
    }

    /**
     * Unregisters the incoming channel for a certain {@link PluginMessageListener}
     */
    default void unregisterIncomingChannel(final PluginMessageListener listener)
    {
        this.getChannel().ifPresent(channel -> Bukkit.getMessenger().unregisterIncomingPluginChannel(AntiCheatAddition.getInstance(), channel, listener));
    }

    /**
     * Registers the outgoing channel for a certain {@link PluginMessageListener}
     */
    default void registerOutgoingChannel()
    {
        this.getChannel().ifPresent(channel -> Bukkit.getMessenger().registerOutgoingPluginChannel(AntiCheatAddition.getInstance(), channel));
    }

    /**
     * Unregisters the outgoing channel for a certain {@link PluginMessageListener}
     */
    default void unregisterOutgoingChannel()
    {
        this.getChannel().ifPresent(channel -> Bukkit.getMessenger().unregisterOutgoingPluginChannel(AntiCheatAddition.getInstance(), channel));
    }
}

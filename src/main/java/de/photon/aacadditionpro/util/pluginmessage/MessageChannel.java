package de.photon.aacadditionpro.util.pluginmessage;

import com.comphenix.protocol.wrappers.MinecraftKey;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.ServerVersion;
import lombok.Getter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Objects;

public class MessageChannel extends MinecraftKey
{
    public static final MessageChannel MC_BRAND_CHANNEL = MessageChannel.of("minecraft", "brand");

    @Getter private final String legacyName;

    private MessageChannel(String prefix, String key, String legacyName)
    {
        super(prefix, key);
        this.legacyName = legacyName;
    }

    private MessageChannel(String prefix, String key)
    {
        super(prefix, key);

        val tempKey = this.getKey();
        val upperStartTempKey = Character.toUpperCase(tempKey.charAt(0)) + tempKey.substring(1);
        switch (this.getPrefix()) {
            case "minecraft":
                this.legacyName = "MC|" + upperStartTempKey;
                break;
            case "px":
                this.legacyName = "PX|" + upperStartTempKey;
                break;
            case "wdl":
                this.legacyName = "WDL|" + tempKey.toUpperCase();
                break;
            default:
                // This is done to make sure that 1.13.2 servers with plugins that utilize PluginMessageChannels
                // do not see a lot of exceptions.
                this.legacyName = null;
                break;
        }
    }

    private MessageChannel(MinecraftKey minecraftKey)
    {
        this(minecraftKey.getPrefix(), minecraftKey.getKey());
    }

    public static MessageChannel of(MinecraftKey minecraftKey)
    {
        return new MessageChannel(minecraftKey);
    }

    public static MessageChannel of(String prefix, String key)
    {
        return new MessageChannel(prefix, key);
    }

    public static MessageChannel of(String prefix, String key, String legacyName)
    {
        return new MessageChannel(prefix, key, legacyName);
    }

    /**
     * This gets the correct channel for the server version.
     */
    public String getChannel()
    {
        return ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS.contains(ServerVersion.getActiveServerVersion()) ?
               this.legacyName :
               this.getFullKey();
    }

    /**
     * Registers the incoming channel for a certain {@link PluginMessageListener}
     */
    public void registerIncomingChannel(final PluginMessageListener listener)
    {
        Bukkit.getMessenger().registerIncomingPluginChannel(AACAdditionPro.getInstance(), this.getChannel(), listener);
    }

    /**
     * Unregisters the incoming channel for a certain {@link PluginMessageListener}
     */
    public void unregisterIncomingChannel(final PluginMessageListener listener)
    {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(AACAdditionPro.getInstance(), this.getChannel(), listener);
    }

    /**
     * Registers the outgoing channel for a certain {@link PluginMessageListener}
     */
    public void registerOutgoingChannel()
    {
        Bukkit.getMessenger().registerOutgoingPluginChannel(AACAdditionPro.getInstance(), this.getChannel());
    }

    /**
     * Unregisters the outgoing channel for a certain {@link PluginMessageListener}
     */
    public void unregisterOutgoingChannel()
    {
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(AACAdditionPro.getInstance(), this.getChannel());
    }

    @Override
    public boolean equals(Object o)
    {
        // Manual equals() and hashCode() as we need to access the super class which does not specify them.
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageChannel that = (MessageChannel) o;
        return Objects.equals(this.getPrefix(), that.getPrefix()) && Objects.equals(this.getKey(), that.getKey());
    }

    @Override
    public int hashCode()
    {
        // Manual equals() and hashCode() as we need to access the super class which does not specify them.
        return Objects.hash(this.getPrefix(), this.getKey());
    }
}

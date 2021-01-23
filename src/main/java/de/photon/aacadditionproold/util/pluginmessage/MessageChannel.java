package de.photon.aacadditionproold.util.pluginmessage;

import com.comphenix.protocol.wrappers.MinecraftKey;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.ServerVersion;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class MessageChannel extends MinecraftKey
{
    public static final MessageChannel MC_BRAND_CHANNEL = new MessageChannel("minecraft", "brand");

    @Getter
    private final String legacyName;

    public MessageChannel(String prefix, String key, String legacyName)
    {
        super(prefix, key);
        this.legacyName = legacyName;
    }

    public MessageChannel(String prefix, String key)
    {
        super(prefix, key);

        String tempKey = this.getKey();
        switch (this.getPrefix()) {
            case "minecraft":
                tempKey = Character.toUpperCase(tempKey.charAt(0)) + tempKey.substring(1);
                this.legacyName = "MC|" + tempKey;
                break;
            case "px":
                tempKey = Character.toUpperCase(tempKey.charAt(0)) + tempKey.substring(1);
                this.legacyName = "PX|" + tempKey;
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

    public MessageChannel(MinecraftKey minecraftKey)
    {
        this(minecraftKey.getPrefix(), minecraftKey.getKey());
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
}

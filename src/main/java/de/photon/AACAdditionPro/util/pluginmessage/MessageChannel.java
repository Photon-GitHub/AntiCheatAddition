package de.photon.AACAdditionPro.util.pluginmessage;

import com.comphenix.protocol.wrappers.MinecraftKey;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ServerVersion;
import lombok.Getter;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class MessageChannel extends MinecraftKey
{
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
                throw new IllegalArgumentException("Legacy plugin message conversion is not possible for prefix " + this.getPrefix());
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
     * Registers the channel for a certain {@link PluginMessageListener} (both incoming and outgoing packets)
     */
    public void registerChannel(final PluginMessageListener listener)
    {
        if (ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS.contains(ServerVersion.getActiveServerVersion())) {
            AACAdditionPro.getInstance().getServer().getMessenger().registerIncomingPluginChannel(AACAdditionPro.getInstance(), this.getLegacyName(), listener);
            AACAdditionPro.getInstance().getServer().getMessenger().registerOutgoingPluginChannel(AACAdditionPro.getInstance(), this.getLegacyName());
        }
        else {
            AACAdditionPro.getInstance().getServer().getMessenger().registerIncomingPluginChannel(AACAdditionPro.getInstance(), this.getFullKey(), listener);
            AACAdditionPro.getInstance().getServer().getMessenger().registerOutgoingPluginChannel(AACAdditionPro.getInstance(), this.getFullKey());
        }
    }

    /**
     * Unregisters the channel for a certain {@link PluginMessageListener} (both incoming and outgoing packets)
     */
    public void unregisterChannel(final PluginMessageListener listener)
    {
        if (ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS.contains(ServerVersion.getActiveServerVersion())) {
            AACAdditionPro.getInstance().getServer().getMessenger().unregisterIncomingPluginChannel(AACAdditionPro.getInstance(), this.getLegacyName(), listener);
            AACAdditionPro.getInstance().getServer().getMessenger().unregisterOutgoingPluginChannel(AACAdditionPro.getInstance(), this.getLegacyName());
        }
        else {
            AACAdditionPro.getInstance().getServer().getMessenger().unregisterIncomingPluginChannel(AACAdditionPro.getInstance(), this.getFullKey(), listener);
            AACAdditionPro.getInstance().getServer().getMessenger().unregisterOutgoingPluginChannel(AACAdditionPro.getInstance(), this.getFullKey());
        }
    }
}

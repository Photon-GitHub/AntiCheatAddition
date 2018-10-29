package de.photon.AACAdditionPro.modules;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ServerVersion;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Set;

/**
 * Marks a {@link Module} which utilizes the {@link PluginMessageListener} functionality.
 */
public interface PluginMessageListenerModule extends Module, PluginMessageListener
{
    /**
     * Additional chores needed to enable a {@link PluginMessageListenerModule}
     */
    static void enable(final PluginMessageListenerModule module)
    {
        for (final String channel : ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS.contains(ServerVersion.getActiveServerVersion()) ?
                                    module.getLegacyPluginMessageChannels() :
                                    module.getPluginMessageChannels()) {
            AACAdditionPro.getInstance().getServer().getMessenger().registerIncomingPluginChannel(AACAdditionPro.getInstance(), channel, module);
            AACAdditionPro.getInstance().getServer().getMessenger().registerOutgoingPluginChannel(AACAdditionPro.getInstance(), channel);
        }
    }

    /**
     * Additional chores needed to disable a {@link PluginMessageListenerModule}
     */
    static void disable(final PluginMessageListenerModule module)
    {
        for (final String channel : ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS.contains(ServerVersion.getActiveServerVersion()) ?
                                    module.getLegacyPluginMessageChannels() :
                                    module.getPluginMessageChannels()) {
            AACAdditionPro.getInstance().getServer().getMessenger().unregisterIncomingPluginChannel(AACAdditionPro.getInstance(), channel, module);
            AACAdditionPro.getInstance().getServer().getMessenger().unregisterOutgoingPluginChannel(AACAdditionPro.getInstance(), channel);
        }
    }

    /**
     * This returns the channels the {@link PluginMessageListener} should listen to.
     * Old format for pre-1.13.2 minecraft versions.
     */
    Set<String> getLegacyPluginMessageChannels();

    /**
     * This returns the channels the {@link PluginMessageListener} should listen to.
     */
    Set<String> getPluginMessageChannels();
}

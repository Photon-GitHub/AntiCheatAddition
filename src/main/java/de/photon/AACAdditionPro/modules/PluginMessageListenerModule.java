package de.photon.AACAdditionPro.modules;

import de.photon.AACAdditionPro.util.pluginmessage.MessageChannel;
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
        for (final MessageChannel channel : module.getPluginMessageChannels()) {
            channel.registerChannel(module);
        }
    }

    /**
     * Additional chores needed to disable a {@link PluginMessageListenerModule}
     */
    static void disable(final PluginMessageListenerModule module)
    {
        for (final MessageChannel channel : module.getPluginMessageChannels()) {
            channel.unregisterChannel(module);
        }
    }

    /**
     * This returns the channels the {@link PluginMessageListener} should listen to.
     */
    Set<MessageChannel> getPluginMessageChannels();
}

package de.photon.AACAdditionPro;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketListener;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public interface Module
{
    /**
     * This enables the check by registering it in all Managers.
     * <p>
     * Covered by the default implementation:<br>
     * Bukkit's event {@link Listener},<br>
     * ProtocolLib's {@link PacketListener} or {@link PacketAdapter},<br>
     * Bukkit's {@link PluginMessageListener}
     */
    default void enable()
    {
        // Bukkit event listener
        if (this instanceof Listener) {
            AACAdditionPro.getInstance().registerListener((Listener) this);
        }

        // PacketAdapter register
        if (this instanceof PacketListener) {
            ProtocolLibrary.getProtocolManager().addPacketListener((PacketListener) this);
        }

        // Plugin message channels
        final String[] pluginMessageChannels = this.getPluginMessageChannels();

        if (this instanceof PluginMessageListener && pluginMessageChannels != null) {
            for (final String channel : pluginMessageChannels) {
                AACAdditionPro.getInstance().getServer().getMessenger().registerIncomingPluginChannel(AACAdditionPro.getInstance(), channel, (PluginMessageListener) this);
                AACAdditionPro.getInstance().getServer().getMessenger().registerOutgoingPluginChannel(AACAdditionPro.getInstance(), channel);
            }
        }

        this.subEnable();
    }

    /**
     * This disables the check by unregistering it in all Managers.
     * <p>
     * Covered by the default implementation:<br>
     * Bukkit's event {@link Listener},<br>
     * ProtocolLib's {@link PacketListener} or {@link PacketAdapter},<br>
     * Bukkit's {@link PluginMessageListener}
     */
    default void disable()
    {
        // Bukkit event listener cleanup
        if (this instanceof Listener) {
            HandlerList.unregisterAll((Listener) this);
        }

        // PacketAdapter register cleanup
        if (this instanceof PacketAdapter) {
            ProtocolLibrary.getProtocolManager().removePacketListener((PacketListener) this);
        }

        // Plugin message channels cleanup
        final String[] pluginMessageChannels = this.getPluginMessageChannels();

        if (this instanceof PluginMessageListener && pluginMessageChannels != null) {
            for (final String channel : pluginMessageChannels) {
                AACAdditionPro.getInstance().getServer().getMessenger().unregisterIncomingPluginChannel(AACAdditionPro.getInstance(), channel, (PluginMessageListener) this);
                AACAdditionPro.getInstance().getServer().getMessenger().unregisterOutgoingPluginChannel(AACAdditionPro.getInstance(), channel);
            }
        }
    }

    /**
     * All config values are initialized here and other tasks that are not covered by enable() should be stated here.
     */
    void subEnable();


    /**
     * Cancelling tasks and everything else that is not covered by disable() should be done here.
     */
    void subDisable();

    /**
     * This are the channels a PluginMessageListener should listen to.
     * Only needed to override for PluginMessageListeners.
     * By default it returns null for no registration of plugin channels.
     */
    default String[] getPluginMessageChannels()
    {
        return null;
    }

    /**
     * Displays all the {@link ServerVersion}s a check supports.
     * It will autodisable itself on the server start if the server version matches the excluded {@link ServerVersion}.
     * <p>
     * By default all {@link ServerVersion} are marked as supported.
     */
    default Set<ServerVersion> getSupportedVersions()
    {
        return new HashSet<>(Arrays.asList(ServerVersion.values()));
    }
}

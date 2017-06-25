package de.photon.AACAdditionPro;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketListener;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.multiversion.ServerVersion;
import org.bukkit.Color;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.util.Vector;

import java.lang.reflect.Field;
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
        // Config-Annotation processing
        for (Field field : this.getClass().getDeclaredFields()) {
            final LoadFromConfiguration annotation = field.getAnnotation(LoadFromConfiguration.class);

            if (annotation != null) {
                // Make it possible to modify the field
                final boolean accessible = field.isAccessible();
                field.setAccessible(true);

                // Load the value from the config
                Class clazz = field.getType();

                String prePath = "";

                if (this instanceof AACAdditionProCheck) {
                    prePath = ((AACAdditionProCheck) this).getAdditionHackType().getConfigString();
                }

                // The different classes
                try {
                    // Boolean
                    if (clazz == boolean.class || clazz == Boolean.class) {
                        field.setBoolean(this, AACAdditionPro.getInstance().getConfig().getBoolean(prePath + annotation.configPath()));

                        // Numbers
                    } else if (clazz == double.class || clazz == Double.class) {
                        field.setDouble(this, AACAdditionPro.getInstance().getConfig().getDouble(prePath + annotation.configPath()));
                    } else if (clazz == int.class || clazz == Integer.class) {
                        field.setInt(this, AACAdditionPro.getInstance().getConfig().getInt(prePath + annotation.configPath()));
                    } else if (clazz == long.class || clazz == Long.class) {
                        field.setLong(this, AACAdditionPro.getInstance().getConfig().getLong(prePath + annotation.configPath()));

                        // Strings
                    } else if (clazz == String.class) {
                        field.set(this, AACAdditionPro.getInstance().getConfig().getString(prePath + annotation.configPath()));

                        // Special stuff
                    } else if (clazz == ItemStack.class) {
                        field.set(this, AACAdditionPro.getInstance().getConfig().getItemStack(prePath + annotation.configPath()));
                    } else if (clazz == Color.class) {
                        field.set(this, AACAdditionPro.getInstance().getConfig().getColor(prePath + annotation.configPath()));
                    } else if (clazz == OfflinePlayer.class) {
                        field.set(this, AACAdditionPro.getInstance().getConfig().getOfflinePlayer(prePath + annotation.configPath()));
                    } else if (clazz == Vector.class) {
                        field.set(this, AACAdditionPro.getInstance().getConfig().getVector(prePath + annotation.configPath()));

                        // No special type found
                    } else {
                        field.set(this, AACAdditionPro.getInstance().getConfig().get(prePath + annotation.configPath()));
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

                // Set the field's accessible value again
                field.setAccessible(accessible);
            }
        }

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

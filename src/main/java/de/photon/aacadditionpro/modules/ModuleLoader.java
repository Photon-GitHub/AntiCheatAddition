package de.photon.aacadditionpro.modules;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketListener;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.util.config.ConfigUtils;
import de.photon.aacadditionpro.util.datastructure.batch.BatchProcessor;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Value
public class ModuleLoader
{
    Module module;

    // Startup
    boolean bungeecordForbidden;
    Set<String> pluginDependencies;
    Set<String> pluginIncompatibilities;
    Set<ServerVersion> allowedServerVersions;

    // Loading
    BatchProcessor<?> batchProcessor;
    Set<MessageChannel> incoming;
    Set<MessageChannel> outgoing;

    Set<Listener> listeners;
    Set<PacketListener> packetListeners;

    public static Builder builder(Module module)
    {
        return new Builder(module);
    }

    /**
     * Tries to load the referenced {@link Module}.
     *
     * @return <code>true</code> if the {@link Module} has been loaded and <code>false</code> if it could not be loaded.
     */
    public boolean load()
    {
        if (this.bungeecordForbidden && AACAdditionPro.getInstance().isBungeecord()) {
            DebugSender.getInstance().sendDebug(module.getConfigString() + " is not compatible with bungeecord.", true, false);
            return false;
        }

        val missingDependencies = pluginDependencies.stream().filter(dependency -> !Bukkit.getServer().getPluginManager().isPluginEnabled(dependency)).sorted().collect(Collectors.joining(", "));
        if (!missingDependencies.isEmpty()) {
            DebugSender.getInstance().sendDebug(module.getConfigString() + " has been not been enabled as of missing dependencies. Missing: " + missingDependencies, true, false);
            return false;
        }

        val loadedIncompatibilities = pluginIncompatibilities.stream().filter(dependency -> Bukkit.getServer().getPluginManager().isPluginEnabled(dependency)).sorted().collect(Collectors.joining(", "));
        if (!loadedIncompatibilities.isEmpty()) {
            DebugSender.getInstance().sendDebug(module.getConfigString() + " has been not been enabled as it is incompatible with another plugin on the server. Incompatible plugins: " + loadedIncompatibilities, true, false);
            return false;
        }

        if (!ServerVersion.containsActiveServerVersion(allowedServerVersions)) {
            DebugSender.getInstance().sendDebug(module.getConfigString() + " is not compatible with your server version.", true, false);
            return false;
        }

        if (!AACAdditionPro.getInstance().getConfig().getBoolean(this.module.getConfigString() + ".enabled")) {
            DebugSender.getInstance().sendDebug(module.getConfigString() + " has been disabled in the config.", true, false);
            return false;
        }

        // Load the config values
        ConfigUtils.processLoadFromConfiguration(module, module.getConfigString());

        // Handle Listeners and PacketListeners
        for (Listener listener : listeners) AACAdditionPro.getInstance().registerListener(listener);
        for (PacketListener packetListener : packetListeners) ProtocolLibrary.getProtocolManager().addPacketListener(packetListener);

        if (batchProcessor != null) batchProcessor.enable();

        for (MessageChannel messageChannel : incoming) messageChannel.registerIncomingChannel((PluginMessageListener) module);
        for (MessageChannel messageChannel : outgoing) messageChannel.registerOutgoingChannel();

        DebugSender.getInstance().sendDebug(module.getConfigString() + " has been enabled.", true, false);
        return true;
    }

    public void unload()
    {
        // Handle Listeners and PacketListeners
        for (Listener listener : listeners) HandlerList.unregisterAll(listener);
        for (PacketListener packetListener : packetListeners) ProtocolLibrary.getProtocolManager().removePacketListener(packetListener);

        if (batchProcessor != null) batchProcessor.disable();

        for (MessageChannel messageChannel : incoming) messageChannel.unregisterIncomingChannel((PluginMessageListener) module);
        for (MessageChannel messageChannel : outgoing) messageChannel.unregisterOutgoingChannel();
    }

    @RequiredArgsConstructor
    public static class Builder
    {
        private final Module module;
        private final ImmutableSet.Builder<Listener> listeners = ImmutableSet.builder();
        private final ImmutableSet.Builder<PacketListener> packetListeners = ImmutableSet.builder();
        private final ImmutableSet.Builder<String> pluginDependencies = ImmutableSet.builder();
        private final ImmutableSet.Builder<String> pluginIncompatibilities = ImmutableSet.builder();
        private final ImmutableSet.Builder<MessageChannel> incoming = ImmutableSet.builder();
        private final ImmutableSet.Builder<MessageChannel> outgoing = ImmutableSet.builder();
        private final Set<ServerVersion> allowedServerVersions = EnumSet.noneOf(ServerVersion.class);
        private boolean bungeecordForbidden = false;
        private BatchProcessor<?> batchProcessor = null;

        public Builder disallowBungeeCord()
        {
            this.bungeecordForbidden = true;
            return this;
        }

        public Builder addListeners(Listener... listeners)
        {
            this.listeners.add(listeners);
            return this;
        }

        public Builder addPacketListeners(PacketListener... packetListeners)
        {
            this.packetListeners.add(packetListeners);
            return this;
        }

        public Builder addPluginDependencies(String... dependencies)
        {
            this.pluginDependencies.add(dependencies);
            return this;
        }

        public Builder addPluginIncompatibilities(String... incompatibilities)
        {
            this.pluginDependencies.add(incompatibilities);
            return this;
        }

        public Builder addIncomingMessageChannels(MessageChannel... channels)
        {
            this.incoming.add(channels);
            return this;
        }

        public Builder addIncomingMessageChannels(Collection<MessageChannel> channels)
        {
            this.incoming.addAll(channels);
            return this;
        }

        public Builder addOutgoingMessageChannels(MessageChannel... channels)
        {
            this.outgoing.add(channels);
            return this;
        }

        public Builder addOutgoingMessageChannels(Collection<MessageChannel> channels)
        {
            this.outgoing.addAll(channels);
            return this;
        }

        public Builder addAllowedServerVersions(ServerVersion... serverVersions)
        {
            Collections.addAll(this.allowedServerVersions, serverVersions);
            return this;
        }

        public Builder addAllowedServerVersions(Collection<ServerVersion> serverVersions)
        {
            this.allowedServerVersions.addAll(serverVersions);
            return this;
        }

        public Builder batchProcessor(BatchProcessor<?> batchProcessor)
        {
            this.batchProcessor = batchProcessor;
            return this;
        }


        public ModuleLoader build()
        {
            val incomingChannels = incoming.build();
            val outgoingChannels = outgoing.build();
            Preconditions.checkArgument((module instanceof PluginMessageListener) == !(incomingChannels.isEmpty() && outgoingChannels.isEmpty()), "Channels have to be registered in a PluginMessageListener Module and cannot be registered otherwise.");

            if (module instanceof Listener) this.listeners.add((Listener) module);
            val resultingVersionSet = allowedServerVersions.isEmpty() ? ServerVersion.ALL_SUPPORTED_VERSIONS : allowedServerVersions;
            return new ModuleLoader(module,
                                    bungeecordForbidden,
                                    pluginDependencies.build(),
                                    pluginIncompatibilities.build(),
                                    Sets.immutableEnumSet(resultingVersionSet),
                                    batchProcessor,
                                    incomingChannels,
                                    outgoingChannels,
                                    listeners.build(),
                                    packetListeners.build());
        }
    }
}
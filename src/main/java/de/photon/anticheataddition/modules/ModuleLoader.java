package de.photon.anticheataddition.modules;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketListener;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.datastructure.batch.BatchProcessor;
import de.photon.anticheataddition.util.messaging.Log;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import lombok.AllArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@AllArgsConstructor
public final class ModuleLoader
{
    @NotNull private final Module module;

    // Startup
    private final boolean bungeecordForbidden;
    @NotNull private final Set<String> pluginDependencies;
    @NotNull private final Set<String> pluginIncompatibilities;
    @NotNull private final Set<ServerVersion> allowedServerVersions;

    // Loading
    @Nullable private final BatchProcessor<?> batchProcessor;
    @NotNull private final Set<MessageChannel> incoming;
    @NotNull private final Set<MessageChannel> outgoing;

    @NotNull private final Set<Listener> listeners;
    @NotNull private final Set<PacketListener> packetListeners;

    public static ModuleLoader of(Module module, PacketAdapter adapter)
    {
        return builder(module).addPacketListeners(adapter).build();
    }

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
        if (this.bungeecordForbidden && AntiCheatAddition.getInstance().isBungeecord()) {
            Log.info(() -> module.getConfigString() + " is not compatible with bungeecord.");
            return false;
        }

        val missingDependencies = pluginDependencies.stream().filter(dependency -> !Bukkit.getServer().getPluginManager().isPluginEnabled(dependency)).sorted().collect(Collectors.joining(", "));
        if (!missingDependencies.isEmpty()) {
            Log.info(() -> module.getConfigString() + " has been not been enabled as of missing dependencies. Missing: " + missingDependencies);
            return false;
        }

        val loadedIncompatibilities = pluginIncompatibilities.stream().filter(incompatibility -> Bukkit.getServer().getPluginManager().isPluginEnabled(incompatibility)).sorted().collect(Collectors.joining(", "));
        if (!loadedIncompatibilities.isEmpty()) {
            Log.info(() -> module.getConfigString() + " has been not been enabled as it is incompatible with another plugin on the server. Incompatible plugins: " + loadedIncompatibilities);
            return false;
        }

        if (!ServerVersion.containsActive(allowedServerVersions)) {
            Log.info(() -> module.getConfigString() + " is not compatible with your server version.");
            return false;
        }

        if (!this.module.loadBoolean(".enabled", false)) {
            Log.info(() -> module.getConfigString() + " has been disabled in the config.");
            return false;
        }

        // Handle Listeners and PacketListeners
        for (Listener listener : listeners) AntiCheatAddition.getInstance().registerListener(listener);
        for (PacketListener packetListener : packetListeners) ProtocolLibrary.getProtocolManager().addPacketListener(packetListener);

        if (batchProcessor != null) batchProcessor.enable();

        for (MessageChannel messageChannel : incoming) messageChannel.registerIncomingChannel((PluginMessageListener) module);
        for (MessageChannel messageChannel : outgoing) messageChannel.registerOutgoingChannel();

        Log.info(() -> module.getConfigString() + " has been enabled.");
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

    public static class Builder
    {
        private final Module module;
        private final ImmutableSet.Builder<Listener> listeners = ImmutableSet.builder();
        private final ImmutableSet.Builder<PacketListener> packetListeners = ImmutableSet.builder();
        private final ImmutableSet.Builder<String> pluginDependencies = ImmutableSet.builder();
        private final ImmutableSet.Builder<String> pluginIncompatibilities = ImmutableSet.builder();
        private final ImmutableSet.Builder<MessageChannel> incoming = ImmutableSet.builder();
        private final ImmutableSet.Builder<MessageChannel> outgoing = ImmutableSet.builder();
        private final Set<ServerVersion> allowedServerVersions = EnumSet.allOf(ServerVersion.class);
        private boolean bungeecordForbidden = false;
        private BatchProcessor<?> batchProcessor = null;

        private Builder(Module module) {this.module = module;}

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

        public Builder addIncomingMessageChannel(MessageChannel channel)
        {
            this.incoming.add(channel);
            return this;
        }

        public Builder addOutgoingMessageChannel(MessageChannel channel)
        {
            this.outgoing.add(channel);
            return this;
        }

        public Builder setAllowedServerVersions(Collection<ServerVersion> serverVersions)
        {
            this.allowedServerVersions.clear();
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
            Preconditions.checkArgument((module instanceof PluginMessageListener) == !(incomingChannels.isEmpty()), "Incoming channels have to be registered in a PluginMessageListener Module and cannot be registered otherwise.");

            if (module instanceof Listener listener) this.listeners.add(listener);

            return new ModuleLoader(module,
                                    bungeecordForbidden,
                                    pluginDependencies.build(),
                                    pluginIncompatibilities.build(),
                                    Sets.immutableEnumSet(allowedServerVersions),
                                    batchProcessor,
                                    incomingChannels,
                                    outgoing.build(),
                                    listeners.build(),
                                    packetListeners.build());
        }
    }
}
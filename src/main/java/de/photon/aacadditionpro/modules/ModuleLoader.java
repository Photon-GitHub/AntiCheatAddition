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
import java.util.HashSet;
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

    boolean listener;
    boolean packetListener;

    public ModuleLoader(Module module, boolean bungeecordForbidden, Set<String> pluginDependencies, Set<String> pluginIncompatibilities, Set<ServerVersion> allowedServerVersions, BatchProcessor<?> batchProcessor, Set<MessageChannel> incoming, Set<MessageChannel> outgoing)
    {
        this.module = module;
        this.bungeecordForbidden = bungeecordForbidden;
        this.pluginDependencies = ImmutableSet.copyOf(pluginDependencies);
        this.pluginIncompatibilities = ImmutableSet.copyOf(pluginIncompatibilities);
        this.allowedServerVersions = Sets.immutableEnumSet(allowedServerVersions);
        this.batchProcessor = batchProcessor;
        this.incoming = ImmutableSet.copyOf(incoming);
        this.outgoing = ImmutableSet.copyOf(outgoing);

        this.listener = (module instanceof Listener);
        this.packetListener = (module instanceof PacketListener);

        Preconditions.checkArgument((module instanceof PluginMessageListener) == !(incoming.isEmpty() && outgoing.isEmpty()), "Channels have to be registered in a PluginMessageListener Module and cannot be registered otherwise.");
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

        if (!ServerVersion.supportsActiveServerVersion(allowedServerVersions)) {
            DebugSender.getInstance().sendDebug(module.getConfigString() + " is not compatible with your server version.", true, false);
            return false;
        }

        // Load the config values
        ConfigUtils.processLoadFromConfiguration(module, module.getConfigString());

        // Automatically handle Listener and PacketListener
        if (listener) AACAdditionPro.getInstance().registerListener((Listener) module);
        if (packetListener) ProtocolLibrary.getProtocolManager().addPacketListener((PacketListener) module);
        if (batchProcessor != null) batchProcessor.enable();

        if (!incoming.isEmpty()) {
            incoming.forEach(messageChannel -> messageChannel.registerIncomingChannel((PluginMessageListener) module));
        }

        if (!outgoing.isEmpty()) outgoing.forEach(MessageChannel::registerOutgoingChannel);
        return true;
    }

    public void unload()
    {
        // Automatically handle Listener and PacketListener
        if (listener) HandlerList.unregisterAll((Listener) module);
        if (packetListener) ProtocolLibrary.getProtocolManager().removePacketListener((PacketListener) module);
        if (batchProcessor != null) batchProcessor.disable();

        if (!incoming.isEmpty()) {
            incoming.forEach(messageChannel -> messageChannel.unregisterIncomingChannel((PluginMessageListener) module));
        }

        if (!outgoing.isEmpty()) outgoing.forEach(MessageChannel::unregisterOutgoingChannel);
    }

    @RequiredArgsConstructor
    public static class Builder
    {
        private final Module module;
        private final Set<String> pluginDependencies = new HashSet<>();
        private final Set<String> pluginIncompatibilities = new HashSet<>();
        private final Set<MessageChannel> incoming = new HashSet<>();
        private final Set<MessageChannel> outgoing = new HashSet<>();
        private final Set<ServerVersion> allowedServerVersions = EnumSet.noneOf(ServerVersion.class);
        private boolean bungeecordForbidden = false;
        private BatchProcessor<?> batchProcessor = null;

        public Builder disallowBungeeCord()
        {
            this.bungeecordForbidden = true;
            return this;
        }

        public Builder addPluginDependencies(String... dependencies)
        {
            Collections.addAll(this.pluginDependencies, dependencies);
            return this;
        }

        public Builder addPluginIncompatibilities(String... incompatibilities)
        {
            Collections.addAll(this.pluginIncompatibilities, incompatibilities);
            return this;
        }

        public Builder addIncomingMessageChannels(MessageChannel... channels)
        {
            Collections.addAll(this.incoming, channels);
            return this;
        }

        public Builder addIncomingMessageChannels(Collection<MessageChannel> channels)
        {
            this.incoming.addAll(channels);
            return this;
        }

        public Builder addOutgoingMessageChannels(MessageChannel... channels)
        {
            Collections.addAll(this.outgoing, channels);
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
            return new ModuleLoader(module,
                                    bungeecordForbidden,
                                    ImmutableSet.copyOf(pluginDependencies),
                                    ImmutableSet.copyOf(pluginIncompatibilities),
                                    // Make sure to allow all server versions if nothing else is specified.
                                    Sets.immutableEnumSet(allowedServerVersions.isEmpty() ? ServerVersion.ALL_SUPPORTED_VERSIONS : allowedServerVersions),
                                    batchProcessor,
                                    ImmutableSet.copyOf(incoming),
                                    ImmutableSet.copyOf(outgoing));
        }
    }
}
package de.photon.anticheataddition.util.pluginmessage;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EmptyMessageChannel implements MessageChannel
{
    public static final EmptyMessageChannel EMPTY = new EmptyMessageChannel();

    @Override
    public Optional<String> getChannel()
    {
        return Optional.empty();
    }

    @Override
    public void registerIncomingChannel(PluginMessageListener listener)
    {
        // Do nothing here as this channel is empty and does not represent a "real" channel.
    }

    @Override
    public void unregisterIncomingChannel(PluginMessageListener listener)
    {
        // Do nothing here as this channel is empty and does not represent a "real" channel.
    }

    @Override
    public void registerOutgoingChannel()
    {
        // Do nothing here as this channel is empty and does not represent a "real" channel.
    }

    @Override
    public void unregisterOutgoingChannel()
    {
        // Do nothing here as this channel is empty and does not represent a "real" channel.
    }
}

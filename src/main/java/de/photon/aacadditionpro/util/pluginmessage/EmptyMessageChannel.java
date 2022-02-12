package de.photon.aacadditionpro.util.pluginmessage;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class EmptyMessageChannel implements MessageChannel
{
    public static final EmptyMessageChannel EMPTY = new EmptyMessageChannel();

    @Override
    public @NotNull String getChannel()
    {
        return "";
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

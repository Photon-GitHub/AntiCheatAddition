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
    public void registerIncomingChannel(PluginMessageListener listener) {}

    @Override
    public void unregisterIncomingChannel(PluginMessageListener listener) {}

    @Override
    public void registerOutgoingChannel() {}

    @Override
    public void unregisterOutgoingChannel() {}
}

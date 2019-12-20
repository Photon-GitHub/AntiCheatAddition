package de.photon.aacadditionpro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PluginMessageListenerModule;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class LiteLoaderControl extends ClientControlModule implements PluginMessageListenerModule
{
    @Override
    public void onPluginMessageReceived(@NotNull final String channel, @NotNull final Player player, @NotNull final byte[] message)
    {
        final String stringMessage = this.getMCBrandMessage(channel, message);

        if (stringMessage.contains("LiteLoader")) {
            this.executeCommands(player);
        }
    }

    @Override
    public Set<MessageChannel> getIncomingChannels()
    {
        return ImmutableSet.of(MessageChannel.MC_BRAND_CHANNEL);
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.LITELOADER_CONTROL;
    }
}

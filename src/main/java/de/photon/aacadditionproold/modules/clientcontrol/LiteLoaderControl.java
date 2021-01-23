package de.photon.aacadditionproold.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.PluginMessageListenerModule;
import de.photon.aacadditionproold.util.pluginmessage.MessageChannel;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class LiteLoaderControl extends ClientControlModule implements PluginMessageListenerModule
{
    @Getter
    private static final LiteLoaderControl instance = new LiteLoaderControl();

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

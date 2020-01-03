package de.photon.aacadditionpro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PluginMessageListenerModule;
import de.photon.aacadditionpro.modules.RestrictedServerVersion;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ForgeControl extends ClientControlModule implements PluginMessageListenerModule, RestrictedServerVersion
{

    @Override
    public void onPluginMessageReceived(@NotNull final String channel, @NotNull final Player player, @NotNull final byte[] message)
    {
        // MCBrand channel with flag
        if (this.shouldFlagBrandCheck(channel, player, message, "fml", "forge") ||
            // or other channel
            !this.isBrandChannel(channel))
        {
            executeCommands(player);
        }
    }

    @Override
    public Set<MessageChannel> getIncomingChannels()
    {
        return ImmutableSet.of(new MessageChannel("minecraft", "fml", "FML"),
                               new MessageChannel("minecraft", "fmlhs", "FMLHS"),
                               MessageChannel.MC_BRAND_CHANNEL);
    }


    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.FORGE_CONTROL;
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS;
    }
}

package de.photon.AACAdditionPro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PluginMessageListenerModule;
import de.photon.AACAdditionPro.modules.RestrictedServerVersion;
import de.photon.AACAdditionPro.util.pluginmessage.MessageChannel;
import org.bukkit.entity.Player;

import java.util.Set;

public class ForgeControl extends ClientControlModule implements PluginMessageListenerModule, RestrictedServerVersion
{

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
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
    public Set<MessageChannel> getPluginMessageChannels()
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

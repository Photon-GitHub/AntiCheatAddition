package de.photon.aacadditionproold.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionproold.ServerVersion;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.PluginMessageListenerModule;
import de.photon.aacadditionproold.modules.RestrictedServerVersion;
import de.photon.aacadditionproold.util.pluginmessage.MessageChannel;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ForgeControl extends ClientControlModule implements PluginMessageListenerModule, RestrictedServerVersion
{
    @Getter
    private static final ForgeControl instance = new ForgeControl();

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

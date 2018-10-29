package de.photon.AACAdditionPro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PluginMessageListenerModule;
import org.bukkit.entity.Player;

import java.util.Set;

public class ForgeControl extends ClientControlModule implements PluginMessageListenerModule
{

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        // MCBrand channel with flag
        if (this.shouldFlagBrandCheck(channel, player, message, "fml,forge", "fml", "forge") ||
            // or other channel
            !this.isBrandChannel(channel))
        {
            executeCommands(player);
        }
    }

    @Override
    public Set<String> getLegacyPluginMessageChannels()
    {
        return ImmutableSet.of("FML", "FMLHS", MC_BRAND_CHANNEL);
    }

    @Override
    public Set<String> getPluginMessageChannels()
    {
        return ImmutableSet.of("minecraft:FML", "minecraft:FMLHS", MC_BRAND_CHANNEL);
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.FORGE_CONTROL;
    }
}

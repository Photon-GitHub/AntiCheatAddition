package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ClientControlModule;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.List;

public class ForgeControl implements PluginMessageListener, ClientControlModule
{
    @LoadFromConfiguration(configPath = ".commands_on_detection", listType = String.class)
    private List<String> commandsOnDetection;

    private static final String[] FORGEFLAGS = {
            "fml,forge",
            "fml",
            "forge"
    };

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        // MCBrand channel with flag
        if (ClientControlModule.shouldFlagBrandCheck(channel, player, message, FORGEFLAGS) ||
            // or other channel
            !ClientControlModule.isBrandChannel(channel))
        {
            executeCommands(player);
        }
    }

    @Override
    public List<String> getCommandsOnDetection()
    {
        return commandsOnDetection;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.FORGE_CONTROL;
    }

    @Override
    public String[] getPluginMessageChannels()
    {
        return new String[]{
                "FML",
                "FMLHS",
                MC_BRAND_CHANNEL
        };
    }
}

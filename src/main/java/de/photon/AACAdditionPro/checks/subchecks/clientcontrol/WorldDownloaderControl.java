package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ClientControlModule;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.List;

public class WorldDownloaderControl implements PluginMessageListener, ClientControlModule
{
    @LoadFromConfiguration(configPath = ".commands_on_detection", listType = String.class)
    private List<String> commandsOnDetection;

    @Override
    public List<String> getCommandsOnDetection()
    {
        return commandsOnDetection;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.WORLDDOWNLOAD_CONTROL;
    }

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        // MCBrand channel with flag
        if (ClientControlModule.shouldFlagBrandCheck(channel, player, message, "worlddownloader-vanilla") ||
            // or other channel
            !ClientControlModule.isBrandChannel(channel))
        {
            executeCommands(player);
        }
    }

    @Override
    public String[] getPluginMessageChannels()
    {
        return new String[]{
                "WDL|INIT",
                "WDL|CONTROL",
                "WDL|REQUEST",
                MC_BRAND_CHANNEL
        };
    }
}

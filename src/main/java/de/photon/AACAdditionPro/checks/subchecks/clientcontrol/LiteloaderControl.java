package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ClientControlModule;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.List;

public class LiteloaderControl implements PluginMessageListener, ClientControlModule
{
    @LoadFromConfiguration(configPath = ".commands_on_detection", listType = String.class)
    private List<String> commandsOnDetection;

    private static final String[] LITELOADERFLAGS = {
            "LiteLoader"
    };

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        if (this.shouldFlagBrandCheck(channel, player, message, LITELOADERFLAGS))
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
        return ModuleType.LITELOADER_CONTROL;
    }

    @Override
    public String[] getPluginMessageChannels()
    {
        return new String[]{MC_BRAND_CHANNEL};
    }
}

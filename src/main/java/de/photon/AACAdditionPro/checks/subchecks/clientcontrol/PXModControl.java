package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ClientControlModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.List;

public class PXModControl implements ClientControlModule, PluginMessageListener
{
    @LoadFromConfiguration(configPath = ".commands_on_detection", listType = String.class)
    private List<String> commandsOnDetection;

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        if (User.isUserInvalid(user, this.getModuleType()))
        {
            return;
        }

        executeCommands(player);
    }

    @Override
    public String[] getPluginMessageChannels()
    {
        return new String[]{"PX|Version"};
    }

    @Override
    public List<String> getCommandsOnDetection()
    {
        return commandsOnDetection;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.BETTERSPRINTING_CONTROL;
    }
}

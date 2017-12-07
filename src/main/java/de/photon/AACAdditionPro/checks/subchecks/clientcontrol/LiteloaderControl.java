package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ClientControlModule;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
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

    // Plugin -> Client Channel for disabling mods: PERMISSIONSREPL
    // Client -> Plugin Channel: PERMISSIONSREPL

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        if (User.isUserInvalid(user)) {
            return;
        }

        // Bypassed players are already filtered out.
        boolean flag = true;

        // MC-Brand for vanilla world-downloader
        final String brand = ClientControlModule.getBrand(channel, message);

        if (brand != null) {
            flag = ClientControlModule.stringContainsFlag(brand, LITELOADERFLAGS) || brand.contains("Lite");
        }

        // Should flag
        if (flag) {
            executeCommands(user.getPlayer());
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
        return new String[]{MCBRANDCHANNEL, "PERMISSIONSREPL"};
    }
}

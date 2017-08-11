package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.checks.ClientControlCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.List;

public class WorldDownloaderControl implements PluginMessageListener, ClientControlCheck
{
    @LoadFromConfiguration(configPath = ".commands_on_detection", listType = String.class)
    private List<String> commandsOnDetection;

    private static final String[] WDLFLAGS = {
            "worlddownloader-vanilla"
    };

    @Override
    public List<String> getCommandsOnDetection()
    {
        return commandsOnDetection;
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.WORLDDOWNLOAD_CONTROL;
    }

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        if (AACAdditionProCheck.isUserInvalid(user)) {
            return;
        }

        // Bypassed players are already filtered out.
        boolean flag = true;

        // MC-Brand for vanilla world-downloader
        if (ClientControlCheck.isBranded(channel)) {
            flag = ClientControlCheck.brandContains(channel, message, WDLFLAGS);
        }

        // Should flag
        if (flag) {
            executeCommands(user.getPlayer());
        }
    }

    @Override
    public String[] getPluginMessageChannels()
    {
        return new String[]{
                "WDL|INIT",
                "WDL|CONTROL",
                "WDL|REQUEST",
                MCBRANDCHANNEL
        };
    }
}

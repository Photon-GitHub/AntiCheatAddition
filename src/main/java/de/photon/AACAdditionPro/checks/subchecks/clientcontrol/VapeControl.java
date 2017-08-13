package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.checks.ClientControlCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.List;

public class VapeControl implements Listener, PluginMessageListener, ClientControlCheck
{
    @LoadFromConfiguration(configPath = ".commands_on_detection", listType = String.class)
    private List<String> commandsOnDetection;

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

    @EventHandler
    public void on(PlayerJoinEvent event)
    {
        event.getPlayer().sendMessage("§8 §8 §1 §3 §3 §7 §8 ");
    }

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        if (AACAdditionProCheck.isUserInvalid(user)) {
            return;
        }

        String clientData;

        try {
            clientData = new String(message);
        } catch (Exception e) {
            clientData = "";
        }

        VerboseSender.sendVerboseMessage("Player " + player.getName() + " joined with Vape | Data: " + clientData);
        executeCommands(player);
    }

    @Override
    public String[] getPluginMessageChannels()
    {
        return new String[]{
                "LOLIMAHCKER"
        };
    }
}

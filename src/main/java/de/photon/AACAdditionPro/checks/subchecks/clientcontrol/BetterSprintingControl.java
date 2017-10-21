package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ClientControlCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.List;

public class BetterSprintingControl implements PluginMessageListener, ClientControlCheck
{
    @LoadFromConfiguration(configPath = ".commands_on_detection", listType = String.class)
    private List<String> commandsOnDetection;

    @LoadFromConfiguration(configPath = ".disable")
    private boolean disable;

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        if (User.isUserInvalid(user)) {
            return;
        }

        // Bypassed players are already filtered out.
        // The mod provides a method to disable it
        if (disable) {
            final ByteArrayDataOutput out1 = ByteStreams.newDataOutput();
            out1.writeByte(1);

            // The channel is always BSM, the right one.
            user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), channel, out1.toByteArray());
        }

        executeCommands(user.getPlayer());
    }

    @Override
    public String[] getPluginMessageChannels()
    {
        return new String[]{"BSM"};
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

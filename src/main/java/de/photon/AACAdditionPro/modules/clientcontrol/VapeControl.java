package de.photon.AACAdditionPro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.modules.ListenerModule;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PluginMessageListenerModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.VerboseSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Set;

public class VapeControl extends ClientControlModule implements ListenerModule, PluginMessageListenerModule
{
    @EventHandler
    public void on(PlayerJoinEvent event)
    {
        event.getPlayer().sendMessage("§8 §8 §1 §3 §3 §7 §8 ");
    }

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        if (User.isUserInvalid(user, this.getModuleType()))
        {
            return;
        }

        String clientData;

        try
        {
            clientData = new String(message);
        } catch (Exception e)
        {
            clientData = "";
        }

        VerboseSender.getInstance().sendVerboseMessage("Player " + player.getName() + " joined with Vape | Data: " + clientData);
        executeCommands(player);
    }

    @Override
    public Set<String> getLegacyPluginMessageChannels()
    {
        return ImmutableSet.of("LOLIMAHCKER");
    }

    @Override
    public Set<String> getPluginMessageChannels()
    {
        return ImmutableSet.of("minecraft:LOLIMAHCKER");
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.VAPE_CONTROL;
    }
}

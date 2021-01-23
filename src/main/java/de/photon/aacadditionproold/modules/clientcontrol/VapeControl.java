package de.photon.aacadditionproold.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionproold.ServerVersion;
import de.photon.aacadditionproold.modules.ListenerModule;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.PluginMessageListenerModule;
import de.photon.aacadditionproold.modules.RestrictedServerVersion;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.UserManager;
import de.photon.aacadditionproold.util.messaging.VerboseSender;
import de.photon.aacadditionproold.util.pluginmessage.MessageChannel;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class VapeControl extends ClientControlModule implements ListenerModule, PluginMessageListenerModule, RestrictedServerVersion
{
    @Getter
    private static final VapeControl instance = new VapeControl();

    @EventHandler
    public void on(PlayerJoinEvent event)
    {
        event.getPlayer().sendMessage("§8 §8 §1 §3 §3 §7 §8 ");
    }

    @Override
    public void onPluginMessageReceived(@NotNull final String channel, final Player player, @NotNull final byte[] message)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        String clientData;

        try {
            clientData = new String(message);
        } catch (Exception e) {
            clientData = "";
        }

        VerboseSender.getInstance().sendVerboseMessage("Player " + player.getName() + " joined with Vape | Data: " + clientData);
        executeCommands(player);
    }

    @Override
    public Set<MessageChannel> getIncomingChannels()
    {
        return ImmutableSet.of(new MessageChannel("minecraft", "lolimahcker", "LOLIMAHCKER"));
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.VAPE_CONTROL;
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS;
    }
}

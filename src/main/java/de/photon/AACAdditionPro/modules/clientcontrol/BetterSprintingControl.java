package de.photon.AACAdditionPro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PluginMessageListenerModule;
import de.photon.AACAdditionPro.modules.RestrictedServerVersion;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.pluginmessage.MessageChannel;
import org.bukkit.entity.Player;

import java.util.Set;

public class BetterSprintingControl extends ClientControlModule implements PluginMessageListenerModule, RestrictedServerVersion
{
    private static final Set<MessageChannel> CHANNELS = ImmutableSet.of(new MessageChannel("minecraft", "bsm", "BSM"),
                                                                        new MessageChannel("minecraft", "bsprint", "BSprint"));

    @LoadFromConfiguration(configPath = ".disable")
    private boolean disable;

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // Bypassed players are already filtered out.
        // The mod provides a method to disable it
        if (disable) {
            // The channel is always BSM, the right one.
            user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), channel, new byte[]{1});
        }

        executeCommands(user.getPlayer());
    }

    @Override
    public Set<MessageChannel> getIncomingChannels()
    {
        return CHANNELS;
    }

    @Override
    public Set<MessageChannel> getOutgoingChannels()
    {
        return CHANNELS;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.BETTERSPRINTING_CONTROL;
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS;
    }
}

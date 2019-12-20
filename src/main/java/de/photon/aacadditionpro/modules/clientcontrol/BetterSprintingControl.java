package de.photon.aacadditionpro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PluginMessageListenerModule;
import de.photon.aacadditionpro.modules.RestrictedServerVersion;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class BetterSprintingControl extends ClientControlModule implements PluginMessageListenerModule, RestrictedServerVersion
{
    private static final Set<MessageChannel> CHANNELS = ImmutableSet.of(new MessageChannel("minecraft", "bsm", "BSM"),
                                                                        new MessageChannel("minecraft", "bsprint", "BSprint"));

    @LoadFromConfiguration(configPath = ".disable")
    private boolean disable;

    @Override
    public void onPluginMessageReceived(@NotNull final String channel, final Player player, @NotNull final byte[] message)
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

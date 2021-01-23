package de.photon.aacadditionproold.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.PluginMessageListenerModule;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.UserManager;
import de.photon.aacadditionproold.util.pluginmessage.MessageChannel;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PXModControl extends ClientControlModule implements PluginMessageListenerModule
{
    @Getter
    private static final PXModControl instance = new PXModControl();

    @Override
    public void onPluginMessageReceived(@NotNull final String channel, final Player player, @NotNull final byte[] message)
    {
        final User user = UserManager.getUser(player.getUniqueId());

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        executeCommands(player);
    }

    @Override
    public Set<MessageChannel> getIncomingChannels()
    {
        return ImmutableSet.of(new MessageChannel("px", "version"));
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.PXMOD_CONTROL;
    }
}

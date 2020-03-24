package de.photon.aacadditionpro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PluginMessageListenerModule;
import de.photon.aacadditionpro.olduser.UserManager;
import de.photon.aacadditionpro.olduser.UserOld;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class PXModControl extends ClientControlModule implements PluginMessageListenerModule
{
    @Override
    public void onPluginMessageReceived(@NotNull final String channel, final Player player, @NotNull final byte[] message)
    {
        final UserOld user = UserManager.getUser(player.getUniqueId());

        if (UserOld.isUserInvalid(user, this.getModuleType())) {
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

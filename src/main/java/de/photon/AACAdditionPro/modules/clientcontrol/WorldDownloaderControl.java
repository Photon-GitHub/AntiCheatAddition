package de.photon.AACAdditionPro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PluginMessageListenerModule;
import de.photon.AACAdditionPro.util.pluginmessage.MessageChannel;
import org.bukkit.entity.Player;

import java.util.Set;

public class WorldDownloaderControl extends ClientControlModule implements PluginMessageListenerModule
{
    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        executeCommands(player);
    }

    @Override
    public Set<MessageChannel> getPluginMessageChannels()
    {
        return ImmutableSet.of(new MessageChannel("wdl", "init"),
                               new MessageChannel("wdl", "control"),
                               new MessageChannel("wdl", "request"));
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.WORLDDOWNLOAD_CONTROL;
    }
}

package de.photon.AACAdditionPro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PluginMessageListenerModule;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.pluginmessage.MessageChannel;
import org.bukkit.entity.Player;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Set;

public class WorldDownloaderControl extends ClientControlModule implements PluginMessageListenerModule
{
    private static MessageChannel WDL_CONTROL_CHANNEL = new MessageChannel("wdl", "control");

    @LoadFromConfiguration(configPath = ".disable")
    private boolean disable;

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message)
    {
        if (disable) {
            try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                 ObjectOutputStream output = new ObjectOutputStream(bytes))
            {
                output.writeInt(1);

                output.writeBoolean(false);
                output.writeInt(0);
                output.writeBoolean(false);
                output.writeBoolean(false);
                output.writeBoolean(false);
                output.writeBoolean(false);

                player.sendPluginMessage(AACAdditionPro.getInstance(), WDL_CONTROL_CHANNEL.getChannel(), bytes.toByteArray());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

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

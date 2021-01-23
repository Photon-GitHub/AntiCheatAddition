package de.photon.aacadditionproold.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.PluginMessageListenerModule;
import de.photon.aacadditionproold.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionproold.util.pluginmessage.MessageChannel;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Set;
import java.util.logging.Level;

public class WorldDownloaderControl extends ClientControlModule implements PluginMessageListenerModule
{
    @Getter
    private static final WorldDownloaderControl instance = new WorldDownloaderControl();

    private static final MessageChannel WDL_CONTROL_CHANNEL = new MessageChannel("wdl", "control");

    @LoadFromConfiguration(configPath = ".disable")
    private boolean disable;

    @Override
    public void onPluginMessageReceived(@NotNull final String channel, @NotNull final Player player, @NotNull final byte[] message)
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
                AACAdditionPro.getInstance().getLogger().log(Level.SEVERE, "Could not write the WorldDownloader packet.", e);
            }
        }

        executeCommands(player);
    }

    @Override
    public Set<MessageChannel> getIncomingChannels()
    {
        return ImmutableSet.of(new MessageChannel("wdl", "init"),
                               new MessageChannel("wdl", "request"));
    }

    @Override
    public Set<MessageChannel> getOutgoingChannels()
    {
        return ImmutableSet.of(new MessageChannel("wdl", "control"));
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.WORLDDOWNLOAD_CONTROL;
    }
}

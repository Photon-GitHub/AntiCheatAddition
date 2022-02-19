package de.photon.aacadditionpro.modules.sentinel;

import com.google.common.io.ByteStreams;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class WorldDownloaderSentinel extends SentinelModule implements PluginMessageListener
{
    private static final MessageChannel WDL_CONTROL_CHANNEL = MessageChannel.of("wdl", "control", "WDL|CONTROL");

    @LoadFromConfiguration(configPath = ".disable.general")
    private boolean disable;

    @LoadFromConfiguration(configPath = ".disable.future")
    private boolean disableFuture;

    @LoadFromConfiguration(configPath = ".disable.save_radius")
    private int saveRadius;
    @LoadFromConfiguration(configPath = ".disable.chunk_caching")
    private boolean disableChunkCaching;
    @LoadFromConfiguration(configPath = ".disable.entity_saving")
    private boolean disableEntitySaving;
    @LoadFromConfiguration(configPath = ".disable.tile_entity_saving")
    private boolean disableTileEntitySaving;
    @LoadFromConfiguration(configPath = ".disable.container_saving")
    private boolean disableContainerSaving;

    public WorldDownloaderSentinel()
    {
        super("WorldDownloader");
    }

    @Override
    public void onPluginMessageReceived(@NotNull final String channel, @NotNull final Player player, final byte[] message)
    {
        // This must not be empty as we defined our channel with both a full key and a legacy string.
        val sendChannel = WDL_CONTROL_CHANNEL.getChannel().orElseThrow();

        /*Documentation:
         * https://github.com/Pokechu22/WorldDownloader-Serverside-Companion/blob/master/src/main/java/wdl/WDLPackets.java
         *
         * and
         *
         * https://wiki.vg/User:Pokechu22/World_downloader
         *
         * The first packet specifies, whether new functions are allowed, the second what current functions are allowed.*/
        val packetZero = ByteStreams.newDataOutput();
        packetZero.writeInt(0);
        packetZero.writeBoolean(!disableFuture);
        player.sendPluginMessage(AACAdditionPro.getInstance(), sendChannel, packetZero.toByteArray());

        val packetOne = ByteStreams.newDataOutput();
        packetOne.writeInt(1);

        packetOne.writeBoolean(!disable);
        packetOne.writeInt(saveRadius);
        packetOne.writeBoolean(!disableChunkCaching);
        packetOne.writeBoolean(!disableEntitySaving);
        packetOne.writeBoolean(!disableTileEntitySaving);
        packetOne.writeBoolean(!disableContainerSaving);
        player.sendPluginMessage(AACAdditionPro.getInstance(), sendChannel, packetOne.toByteArray());

        detection(player);
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addIncomingMessageChannel(MessageChannel.of("wdl", "init", "WDL|INIT"))
                           .addOutgoingMessageChannel(WDL_CONTROL_CHANNEL)
                           .build();
    }
}

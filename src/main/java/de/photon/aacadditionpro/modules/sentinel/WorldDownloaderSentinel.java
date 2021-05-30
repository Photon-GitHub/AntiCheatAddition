package de.photon.aacadditionpro.modules.sentinel;

import com.google.common.io.ByteStreams;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import lombok.val;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WorldDownloaderSentinel extends SentinelModule
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
        super("World");
    }

    @Override
    public void onPluginMessageReceived(@NotNull final String channel, @NotNull final Player player, @NotNull final byte[] message)
    {
        /*Documentation:
         * https://github.com/Pokechu22/WorldDownloader-Serverside-Companion/blob/master/src/main/java/wdl/WDLPackets.java
         *
         * and
         *
         * https://wiki.vg/User:Pokechu22/World_downloader
         *
         * The first packet specifies, whether or not new functions are allowed, the second what current functions are allowed.*/
        val packetZero = ByteStreams.newDataOutput();
        packetZero.writeInt(0);
        packetZero.writeBoolean(!disableFuture);
        player.sendPluginMessage(AACAdditionPro.getInstance(), WDL_CONTROL_CHANNEL.getChannel(), packetZero.toByteArray());

        val packetOne = ByteStreams.newDataOutput();
        packetOne.writeInt(1);

        packetOne.writeBoolean(!disable);
        packetOne.writeInt(saveRadius);
        packetOne.writeBoolean(!disableChunkCaching);
        packetOne.writeBoolean(!disableEntitySaving);
        packetOne.writeBoolean(!disableTileEntitySaving);
        packetOne.writeBoolean(!disableContainerSaving);
        player.sendPluginMessage(AACAdditionPro.getInstance(), WDL_CONTROL_CHANNEL.getChannel(), packetOne.toByteArray());

        detection(player);
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addIncomingMessageChannels(MessageChannel.of("wdl", "init", "WDL|INIT"))
                           .addOutgoingMessageChannels(WDL_CONTROL_CHANNEL)
                           .build();
    }
}

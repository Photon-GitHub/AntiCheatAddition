package de.photon.anticheataddition.modules.sentinel;

import com.google.common.io.ByteStreams;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class WorldDownloaderSentinel extends SentinelModule implements PluginMessageListener
{
    private static final MessageChannel WDL_CONTROL_CHANNEL = MessageChannel.of("wdl", "control", "WDL|CONTROL");

    private final boolean disable = loadBoolean(".disable.general", true);
    private final boolean disableFuture = loadBoolean(".disable.future", true);
    private final int saveRadius = loadInt(".disable.save_radius", 0);
    private final boolean disableChunkCaching = loadBoolean(".disable.chunk_caching", true);
    private final boolean disableEntitySaving = loadBoolean(".disable.entity_saving", true);
    private final boolean disableTileEntitySaving = loadBoolean(".disable.tile_entity_saving", true);
    private final boolean disableContainerSaving = loadBoolean(".disable.container_saving", true);

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
        player.sendPluginMessage(AntiCheatAddition.getInstance(), sendChannel, packetZero.toByteArray());

        val packetOne = ByteStreams.newDataOutput();
        packetOne.writeInt(1);

        packetOne.writeBoolean(!disable);
        packetOne.writeInt(saveRadius);
        packetOne.writeBoolean(!disableChunkCaching);
        packetOne.writeBoolean(!disableEntitySaving);
        packetOne.writeBoolean(!disableTileEntitySaving);
        packetOne.writeBoolean(!disableContainerSaving);
        player.sendPluginMessage(AntiCheatAddition.getInstance(), sendChannel, packetOne.toByteArray());

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

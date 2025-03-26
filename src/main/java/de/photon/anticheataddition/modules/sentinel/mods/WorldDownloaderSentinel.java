package de.photon.anticheataddition.modules.sentinel.mods;

import com.google.common.io.ByteStreams;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.sentinel.SentinelModule;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public final class WorldDownloaderSentinel extends SentinelModule implements PluginMessageListener
{
    public static final WorldDownloaderSentinel INSTANCE = new WorldDownloaderSentinel();

    private static final MessageChannel WDL_CONTROL_CHANNEL = MessageChannel.of("wdl", "control", "WDL|CONTROL");
    public static final MessageChannel WDL_INIT_CHANNEL = MessageChannel.of("wdl", "init", "WDL|INIT");

    private final boolean disable = loadBoolean(".disable.general", true);
    private final boolean disableFuture = loadBoolean(".disable.future", true);
    private final int saveRadius = loadInt(".disable.save_radius", 0);
    private final boolean disableChunkCaching = loadBoolean(".disable.chunk_caching", true);
    private final boolean disableEntitySaving = loadBoolean(".disable.entity_saving", true);
    private final boolean disableTileEntitySaving = loadBoolean(".disable.tile_entity_saving", true);
    private final boolean disableContainerSaving = loadBoolean(".disable.container_saving", true);

    private WorldDownloaderSentinel()
    {
        super("WorldDownloader");
    }

    @Override
    public void onPluginMessageReceived(@NotNull final String channel, @NotNull final Player player, final byte[] message)
    {
        // This must not be empty as we defined our channel with both a full key and a legacy string.
        final var sendChannel = WDL_CONTROL_CHANNEL.getChannel().orElseThrow();

        /*Documentation:
         * https://github.com/Pokechu22/WorldDownloader-Serverside-Companion/blob/master/src/main/java/wdl/WDLPackets.java
         *
         * and
         *
         * https://wiki.vg/User:Pokechu22/World_downloader
         *
         * The first packet specifies, whether new functions are allowed, the second what current functions are allowed.*/
        sendFutureSupportPacket(player, sendChannel);
        sendControlSettingsPacket(player, sendChannel);

        detection(player);
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addIncomingMessageChannel(WDL_INIT_CHANNEL)
                           .addOutgoingMessageChannel(WDL_CONTROL_CHANNEL)
                           .build();
    }

    // checks whether new functions are allowed
    private void sendFutureSupportPacket(Player player, String channel)
    {
        final var packet = ByteStreams.newDataOutput();
        packet.writeInt(0);
        packet.writeBoolean(!disableFuture);
        player.sendPluginMessage(AntiCheatAddition.getInstance(), channel, packet.toByteArray());
    }

    // checks which current functions are allowed
    private void sendControlSettingsPacket(Player player, String channel)
    {
        final var packet = ByteStreams.newDataOutput();
        packet.writeInt(1);
        packet.writeBoolean(!disable);
        packet.writeInt(saveRadius);
        packet.writeBoolean(!disableChunkCaching);
        packet.writeBoolean(!disableEntitySaving);
        packet.writeBoolean(!disableTileEntitySaving);
        packet.writeBoolean(!disableContainerSaving);
        player.sendPluginMessage(AntiCheatAddition.getInstance(), channel, packet.toByteArray());
    }
}

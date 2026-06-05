package de.photon.anticheataddition.modules.additions;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.configuration.server.WrapperConfigServerPluginMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.util.execute.Placeholders;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.pluginmessage.ByteBufUtil;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import de.photon.anticheataddition.util.protocol.PacketAdapterBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitTask;

public final class BrandHider extends Module implements PacketListener, Listener {
    public static final BrandHider INSTANCE = new BrandHider();

    private String brand;
    private final String channel = MessageChannel.MC_BRAND_CHANNEL.getChannel().orElseThrow();
    private BukkitTask updateTask;

    private BrandHider()
    {
        super("BrandHider");
    }

    public void setBrand(String brand)
    {
        this.brand = ChatColor.translateAlternateColorCodes('&', brand) + ChatColor.RESET;
        this.updateAllBrands();
    }

    private void updateAllBrands()
    {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) updateBrand(onlinePlayer);
    }

    private void updateBrand(final Player player)
    {
        // Just send an empty brand message, as this will be replaced by the Packet Adapter below anyways.
        final byte[] payload = createBrandPayload("");
        PacketEvents.getAPI().getPlayerManager().sendPacket(player, new WrapperPlayServerPluginMessage(this.channel, payload));
    }

    private static byte[] createBrandPayload(final String brand)
    {
        final ByteBuf buf = Unpooled.buffer();
        try {
            ByteBufUtil.writeString(buf, brand);
            return ByteBufUtil.toArray(buf);
        } finally {
            buf.release();
        }
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event)
    {
        updateBrand(event.getPlayer());
    }

    @Override
    public void enable()
    {
        Log.finer(() -> "BrandHider brand " + this.brand + " | channel " + this.channel);
        this.setBrand(loadString(".brand", "Some Spigot"));

        final long refreshRate = loadLong(".refresh_rate", 0);
        if (refreshRate > 0) {
            this.updateTask = Bukkit.getScheduler().runTaskTimer(AntiCheatAddition.getInstance(), this::updateAllBrands, 20L, refreshRate);
        }
    }

    @Override
    public void disable()
    {
        if (this.updateTask != null) this.updateTask.cancel();
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           //
                           .addPacketListeners(
                                   // For Configuration.Server, the player might still be null, as they are currently being constructed.
                                   PacketAdapterBuilder.of(this, PacketType.Configuration.Server.PLUGIN_MESSAGE).onSendingRaw((event) -> {
                                       final WrapperConfigServerPluginMessage packet = new WrapperConfigServerPluginMessage(event);
                                       final String channel = packet.getChannelName();
                                       Log.finer(() -> "BrandHider got PLUGIN_MESSAGE in channel " + channel + " | equals: " + this.channel.equals(channel));

                                       if (this.channel.equals(channel)) {
                                           // Only replace non-player placeholders
                                           final String brand = Placeholders.replacePlaceholdersSafely(this.brand);
                                           packet.setData(createBrandPayload(brand));
                                           event.markForReEncode(true);
                                       }
                                   }).build(),
                                   // For Play.Server, the player is already fully initialized on the server, so non-null.
                                   PacketAdapterBuilder.of(this, PacketType.Play.Server.PLUGIN_MESSAGE).onSendingRaw((event) -> {
                                       final WrapperPlayServerPluginMessage packet = new WrapperPlayServerPluginMessage(event);
                                       final String channel = packet.getChannelName();
                                       Log.finer(() -> "BrandHider got PLUGIN_MESSAGE in channel " + channel + " | equals: " + this.channel.equals(channel));

                                       if (this.channel.equals(channel)) {
                                           // Replace all placeholders.
                                           final String brand = Placeholders.replacePlaceholders(this.brand, event.getPlayer());
                                           packet.setData(createBrandPayload(brand));
                                           event.markForReEncode(true);
                                       }
                                   }).build()).build();
    }
}

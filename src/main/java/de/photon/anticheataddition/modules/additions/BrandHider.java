package de.photon.anticheataddition.modules.additions;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.util.execute.Placeholders;
import de.photon.anticheataddition.util.pluginmessage.ByteBufUtil;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import de.photon.anticheataddition.util.reflection.FieldReflect;
import de.photon.anticheataddition.util.reflection.Reflect;
import io.netty.buffer.Unpooled;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class BrandHider extends Module implements Listener
{
    public static final BrandHider INSTANCE = new BrandHider();

    private static final FieldReflect PLAYER_CHANNELS_FIELD = Reflect.fromOBC("entity.CraftPlayer").field("channels");

    private final long refreshRate = this.loadLong(".refresh_rate", 0);

    private String brand;

    public BrandHider()
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
        val buf = Unpooled.buffer();

        ByteBufUtil.writeString(Placeholders.replacePlaceholders(this.brand, player), buf);

        player.sendPluginMessage(AntiCheatAddition.getInstance(), MessageChannel.MC_BRAND_CHANNEL.getChannel().orElseThrow(), ByteBufUtil.toArray(buf));
        buf.release();
    }

    @Override
    public void enable()
    {
        Bukkit.getMessenger().registerOutgoingPluginChannel(AntiCheatAddition.getInstance(), MessageChannel.MC_BRAND_CHANNEL.getChannel().orElseThrow());
        this.setBrand(AntiCheatAddition.getInstance().getConfig().getString(this.getConfigString() + ".brand"));
        if (refreshRate > 0) Bukkit.getScheduler().scheduleSyncRepeatingTask(AntiCheatAddition.getInstance(), this::updateAllBrands, 20, refreshRate);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        // Add the mc brand channel to the player's channels.
        PLAYER_CHANNELS_FIELD.from(event.getPlayer()).asSet(String.class).add(MessageChannel.MC_BRAND_CHANNEL.getChannel().orElseThrow());
        updateBrand(event.getPlayer());
    }
}

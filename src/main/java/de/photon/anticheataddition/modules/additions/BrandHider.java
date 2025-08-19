package de.photon.anticheataddition.modules.additions;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.util.execute.Placeholders;
import de.photon.anticheataddition.util.pluginmessage.ByteBufUtil;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import de.photon.anticheataddition.util.reflection.Reflect;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class BrandHider extends Module implements Listener
{
    public static final BrandHider INSTANCE = new BrandHider();

    private String brand;

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
        final ByteBuf buf = Unpooled.buffer();
        try
        {
            ByteBufUtil.writeString(buf, Placeholders.replacePlaceholders(this.brand, player));

            player.sendPluginMessage(AntiCheatAddition.getInstance(), MessageChannel.MC_BRAND_CHANNEL.getChannel().orElseThrow(), ByteBufUtil.toArray(buf));
        }
        finally
        {
            buf.release();
        }
    }

    @Override
    public void enable()
    {
        Bukkit.getMessenger().registerOutgoingPluginChannel(AntiCheatAddition.getInstance(), MessageChannel.MC_BRAND_CHANNEL.getChannel().orElseThrow());
        this.setBrand(loadString(".brand", "Some Spigot"));

        final long refreshRate = loadLong(".refresh_rate", 0);
        if (refreshRate > 0) Bukkit.getScheduler().runTaskTimer(AntiCheatAddition.getInstance(), this::updateAllBrands, 20, refreshRate);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        // Add the mc brand channel to the player's channels.
        Reflect.fromOBC("entity.CraftPlayer").field("channels").from(event.getPlayer()).asSet(String.class).add(MessageChannel.MC_BRAND_CHANNEL.getChannel().orElseThrow());
        updateBrand(event.getPlayer());
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.MC119.getSupVersionsTo())
                           .build();
    }
}

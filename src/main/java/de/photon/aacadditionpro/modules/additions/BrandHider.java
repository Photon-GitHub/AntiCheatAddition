package de.photon.aacadditionpro.modules.additions;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.execute.Placeholders;
import de.photon.aacadditionpro.util.pluginmessage.ByteBufUtil;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import de.photon.aacadditionpro.util.reflection.FieldReflect;
import de.photon.aacadditionpro.util.reflection.Reflect;
import io.netty.buffer.Unpooled;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Set;

public class BrandHider extends Module implements Listener
{
    public static final BrandHider INSTANCE = new BrandHider();

    private static final FieldReflect PLAYER_CHANNELS_FIELD = Reflect.fromOBC("entity.CraftPlayer").field("channels");

    @LoadFromConfiguration(configPath = ".refresh_rate")
    private long refreshRate;

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

        ByteBufUtil.writeString(Placeholders.replacePlaceholders(this.brand, Set.of(player)), buf);

        player.sendPluginMessage(AACAdditionPro.getInstance(), MessageChannel.MC_BRAND_CHANNEL.getChannel(), ByteBufUtil.toArray(buf));
        buf.release();
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }

    @Override
    public void enable()
    {
        Bukkit.getMessenger().registerOutgoingPluginChannel(AACAdditionPro.getInstance(), MessageChannel.MC_BRAND_CHANNEL.getChannel());
        this.setBrand(AACAdditionPro.getInstance().getConfig().getString(this.getConfigString() + ".brand"));
        if (refreshRate > 0) Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), this::updateAllBrands, 20, refreshRate);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        // Add the mc brand channel to the player's channels.
        PLAYER_CHANNELS_FIELD.from(event.getPlayer()).asSet(String.class).add(MessageChannel.MC_BRAND_CHANNEL.getChannel());
        updateBrand(event.getPlayer());
    }
}

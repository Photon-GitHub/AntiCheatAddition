package de.photon.aacadditionpro.modules.additions;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.util.commands.Placeholders;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.pluginmessage.ByteBufUtil;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import de.photon.aacadditionpro.util.reflection.FieldReflect;
import de.photon.aacadditionpro.util.reflection.Reflect;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

public class BrandHider implements ListenerModule
{
    private final FieldReflect playerChannelsField = Reflect.fromOBC("entity.CraftPlayer").field("channels");
    private static String brand;

    public static void setBrand(String brand)
    {
        BrandHider.brand = ChatColor.translateAlternateColorCodes('&', brand) + ChatColor.RESET;
        updateAllBrands();
    }

    @LoadFromConfiguration(configPath = ".refresh_rate")
    private long refreshRate;

    @Override
    public void enable()
    {
        Bukkit.getMessenger().registerOutgoingPluginChannel(AACAdditionPro.getInstance(), MessageChannel.MC_BRAND_CHANNEL.getChannel());
        setBrand(AACAdditionPro.getInstance().getConfig().getString(this.getConfigString() + ".brand"));

        if (refreshRate > 0) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), BrandHider::updateAllBrands, 20, refreshRate);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        playerChannelsField.from(event.getPlayer()).asSet(String.class).add(MessageChannel.MC_BRAND_CHANNEL.getChannel());
        updateBrand(event.getPlayer());
    }

    private static void updateAllBrands()
    {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            updateBrand(onlinePlayer);
        }
    }

    private static void updateBrand(final Player player)
    {
        ByteBuf byteBuf = Unpooled.buffer();
        final String sentBrand = Placeholders.applyPlaceholders(brand, player, null);
        ByteBufUtil.writeString(sentBrand, byteBuf);
        player.sendPluginMessage(AACAdditionPro.getInstance(), MessageChannel.MC_BRAND_CHANNEL.getChannel(), ByteBufUtil.toArray(byteBuf));
        byteBuf.release();
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.BRAND_HIDER;
    }
}

package de.photon.aacadditionpro.modules.additions;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.execute.Placeholders;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import de.photon.aacadditionpro.util.reflection.FieldReflect;
import de.photon.aacadditionpro.util.reflection.Reflect;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.UnpooledByteBufAllocator;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class BrandHider extends Module implements Listener
{
    private static final FieldReflect PLAYER_CHANNELS_FIELD = Reflect.fromOBC("entity.CraftPlayer").field("channels");
    private static String brand;

    @LoadFromConfiguration(configPath = ".refresh_rate")
    private long refreshRate;

    public BrandHider()
    {
        super("BrandHider");
    }

    public static void setBrand(String brand)
    {
        BrandHider.brand = ChatColor.translateAlternateColorCodes('&', brand) + ChatColor.RESET;
        updateAllBrands();
    }

    private static void updateAllBrands()
    {
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            updateBrand(onlinePlayer);
        }
    }

    private static void updateBrand(final Player player)
    {
        val charBuffer = CharBuffer.wrap(Placeholders.replacePlaceholders(brand, Collections.singleton(player)));
        val buffer = ByteBufUtil.encodeString(UnpooledByteBufAllocator.DEFAULT, charBuffer, StandardCharsets.UTF_8);

        player.sendPluginMessage(AACAdditionPro.getInstance(), MessageChannel.MC_BRAND_CHANNEL.getChannel(), buffer.array());
        buffer.release();
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
        setBrand(AACAdditionPro.getInstance().getConfig().getString(this.getConfigString() + ".brand"));
        if (refreshRate > 0) Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), BrandHider::updateAllBrands, 20, refreshRate);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        // Add the mc brand channel to the player's channels.
        PLAYER_CHANNELS_FIELD.from(event.getPlayer()).asSet(String.class).add(MessageChannel.MC_BRAND_CHANNEL.getChannel());
        updateBrand(event.getPlayer());
    }
}

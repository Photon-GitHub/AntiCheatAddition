package de.photon.AACAdditionPro.modules.additions;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.modules.ListenerModule;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.util.commands.Placeholders;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.pluginmessage.ByteBufUtil;
import de.photon.AACAdditionPro.util.pluginmessage.MessageChannel;
import de.photon.AACAdditionPro.util.reflection.FieldReflect;
import de.photon.AACAdditionPro.util.reflection.Reflect;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

public class BrandHider implements ListenerModule
{
    private FieldReflect playerChannelsField = Reflect.fromOBC("entity.CraftPlayer").field("channels");
    private final String brand = ChatColor.translateAlternateColorCodes('&', AACAdditionPro.getInstance().getConfig().getString(this.getConfigString() + ".brand")) + ChatColor.RESET;

    @LoadFromConfiguration(configPath = ".refresh_rate")
    private long refreshRate;

    @Override
    public void enable()
    {
        Bukkit.getMessenger().registerOutgoingPluginChannel(AACAdditionPro.getInstance(), MessageChannel.MC_BRAND_CHANNEL.getChannel());

        if (refreshRate > 0) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), () -> {
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    this.updateBrand(onlinePlayer);
                }
            }, 20, refreshRate);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        playerChannelsField.from(event.getPlayer()).asSet(String.class).add(MessageChannel.MC_BRAND_CHANNEL.getChannel());
        updateBrand(event.getPlayer());
    }

    private void updateBrand(final Player player)
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

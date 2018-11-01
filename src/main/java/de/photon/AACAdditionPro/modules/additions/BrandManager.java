package de.photon.AACAdditionPro.modules.additions;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PacketListenerModule;
import de.photon.AACAdditionPro.util.packetwrappers.server.WrapperPlayServerCustomPayload;
import de.photon.AACAdditionPro.util.pluginmessage.MessageChannel;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;

import java.nio.charset.StandardCharsets;

public class BrandManager extends PacketAdapter implements PacketListenerModule
{
    private final byte[] brand;

    public BrandManager()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.NORMAL, PacketType.Play.Server.CUSTOM_PAYLOAD);
        brand = Unpooled.copiedBuffer(AACAdditionPro.getInstance().getConfig().getString(this.getConfigString() + ".brand"),
                                      StandardCharsets.UTF_8).array();

        Bukkit.getMessenger().registerOutgoingPluginChannel(AACAdditionPro.getInstance(), MessageChannel.MC_BRAND_CHANNEL.getChannel());
    }

    @Override
    public void onPacketSending(PacketEvent event)
    {
        final WrapperPlayServerCustomPayload customPayload = new WrapperPlayServerCustomPayload(event.getPacket());

        if (customPayload.getChannel().getChannel().equals(MessageChannel.MC_BRAND_CHANNEL.getChannel())) {
            event.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(),
                                                MessageChannel.MC_BRAND_CHANNEL.getChannel(),
                                                brand);
        }
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.BRAND_MANAGER;
    }
}

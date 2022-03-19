package de.photon.anticheataddition.modules.sentinel;

import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import de.photon.anticheataddition.util.pluginmessage.labymod.LabyProtocolUtil;
import io.netty.buffer.Unpooled;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class LabyModSentinel extends SentinelModule implements Listener, PluginMessageListener
{
    private final boolean tablistBanner = loadBoolean(".TablistBanner.enabled", false);
    private final String tablistBannerUrl = loadString(".TablistBanner.url", "");
    private final boolean voicechat = loadBoolean(".Voicechat", true);

    public LabyModSentinel()
    {
        super("LabyMod");
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message)
    {
        val byteBuf = Unpooled.wrappedBuffer(message);
        val key = LabyProtocolUtil.readString(byteBuf, Short.MAX_VALUE);
        //val json = LabyModProtocol.readString(byteBuf, Short.MAX_VALUE);

        // LabyMod user joins the server
        if ("INFO".equals(key)) {
            // Sentinel-commands
            detection(player);

            // Send permissions
            LabyProtocolUtil.sendPermissionMessage(player);

            // Voicechat
            if (!voicechat) LabyProtocolUtil.disableVoiceChat(player);
            if (tablistBanner) LabyProtocolUtil.sendServerBanner(player, tablistBannerUrl);
        }

        byteBuf.release();
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addIncomingMessageChannel(MessageChannel.LABYMOD_CHANNEL)
                           .addOutgoingMessageChannel(MessageChannel.LABYMOD_CHANNEL)
                           .build();
    }
}

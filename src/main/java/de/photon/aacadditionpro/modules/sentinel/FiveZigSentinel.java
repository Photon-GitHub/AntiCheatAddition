package de.photon.aacadditionpro.modules.sentinel;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public class FiveZigSentinel extends SentinelModule implements Listener
{
    private static final int FIVE_ZIG_API_VERSION = 4;
    private static final String REGISTER_SEND_CHANNEL = "the5zigmod:5zig_reg";
    private static final String RESPONSE_CHANNEL = "the5zigmod:5zig";
    private static final ByteBuf MESSAGE = Unpooled.buffer().writeInt(FIVE_ZIG_API_VERSION);

    public FiveZigSentinel()
    {
        super("5Zig");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        val user = User.getUser(event.getPlayer().getUniqueId());
        if (User.isUserInvalid(user, this)) return;

        user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), REGISTER_SEND_CHANNEL, MESSAGE.array());
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message)
    {
        val user = User.getUser(player.getUniqueId());
        if (User.isUserInvalid(user, this)) return;
        detection(user.getPlayer());
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addIncomingMessageChannels(MessageChannel.of(RESPONSE_CHANNEL))
                           .addOutgoingMessageChannels(MessageChannel.of(REGISTER_SEND_CHANNEL))
                           .build();
    }
}

package de.photon.anticheataddition.modules.sentinel.mods;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.sentinel.SentinelModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import io.netty.buffer.Unpooled;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class FiveZigSentinel extends SentinelModule implements Listener, PluginMessageListener
{
    public static final FiveZigSentinel INSTANCE = new FiveZigSentinel();

    private static final int FIVE_ZIG_API_VERSION = 4;
    private static final String REGISTER_SEND_CHANNEL = "the5zigmod:5zig_reg";
    private static final String RESPONSE_CHANNEL = "the5zigmod:5zig";
    private static final byte[] MESSAGE;

    static {
        final var buf = Unpooled.buffer().writeInt(FIVE_ZIG_API_VERSION);
        MESSAGE = Arrays.copyOf(buf.array(), buf.array().length);
        buf.release();
    }

    private FiveZigSentinel()
    {
        super("5Zig");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        user.getPlayer().sendPluginMessage(AntiCheatAddition.getInstance(), REGISTER_SEND_CHANNEL, MESSAGE);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte[] message)
    {
        detection(player);
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addIncomingMessageChannel(MessageChannel.of(RESPONSE_CHANNEL))
                           .addOutgoingMessageChannel(MessageChannel.of(REGISTER_SEND_CHANNEL))
                           .build();
    }
}

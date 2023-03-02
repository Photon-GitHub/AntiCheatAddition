package de.photon.anticheataddition.modules.sentinel.mods;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.sentinel.SentinelModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.pluginmessage.MessageChannel;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.Arrays;

public final class SchematicaSentinel extends SentinelModule implements Listener
{
    public static final SchematicaSentinel INSTANCE = new SchematicaSentinel();

    private final byte[] sentMessage;

    private SchematicaSentinel()
    {
        super("Schematica");
        final ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeByte(0);

        /*
         * This array holds what features of schematica should be disabled.
         * SENDING 1 MEANS ALLOWING THE FEATURE -> NEGATION.
         * Link to the original plugin: https://www.spigotmc.org/resources/schematicaplugin.14411/
         */
        byteBuf.writeBoolean(!loadBoolean(".disable.printer", true));
        byteBuf.writeBoolean(!loadBoolean(".disable.save", true));
        byteBuf.writeBoolean(!loadBoolean(".disable.load", false));

        this.sentMessage = Arrays.copyOf(byteBuf.array(), byteBuf.array().length);
        byteBuf.release();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        MessageChannel.SCHEMATICA_CHANNEL.getChannel().ifPresent(schematicaChannel -> {
            if (user.getPlayer().getListeningPluginChannels().contains(schematicaChannel)) {
                detection(user.getPlayer());
                user.getPlayer().sendPluginMessage(AntiCheatAddition.getInstance(), schematicaChannel, sentMessage);
            }
        });
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addOutgoingMessageChannel(MessageChannel.SCHEMATICA_CHANNEL)
                           .build();
    }
}

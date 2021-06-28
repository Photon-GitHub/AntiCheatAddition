package de.photon.aacadditionpro.modules.sentinel;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import io.netty.buffer.Unpooled;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

public class SchematicaSentinel extends SentinelModule implements Listener
{
    private static final String SCHEMATICA = "schematica";
    private static final MessageChannel SCHEMATICA_CHANNEL = MessageChannel.ofLegacy(SCHEMATICA);

    private final byte[] sentMessage;

    public SchematicaSentinel()
    {
        super("Schematica");
        val byteBuf = Unpooled.buffer();
        byteBuf.writeByte(0);

        /*
         * This array holds what features of schematica should be disabled.
         * SENDING A 1 MEANS ALLOWING THE FEATURE -> NEGATION.
         * Link to the original plugin: https://www.spigotmc.org/resources/schematicaplugin.14411/
         */
        byteBuf.writeBoolean(!AACAdditionPro.getInstance().getConfig().getBoolean(this.getConfigString() + ".disable.printer"));
        byteBuf.writeBoolean(!AACAdditionPro.getInstance().getConfig().getBoolean(this.getConfigString() + ".disable.saveToFile"));
        byteBuf.writeBoolean(!AACAdditionPro.getInstance().getConfig().getBoolean(this.getConfigString() + ".disable.load"));

        this.sentMessage = byteBuf.array();
        byteBuf.release();
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        for (String channel : user.getPlayer().getListeningPluginChannels()) {
            if (channel.equals(SCHEMATICA)) {
                detection(user.getPlayer());
                user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), SCHEMATICA_CHANNEL.getChannel(), sentMessage);
                return;
            }
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addOutgoingMessageChannels(SCHEMATICA_CHANNEL)
                           .build();
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message)
    {
        // Ignore.
    }
}

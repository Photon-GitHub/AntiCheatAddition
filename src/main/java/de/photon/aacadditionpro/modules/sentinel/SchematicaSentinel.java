package de.photon.aacadditionpro.modules.sentinel;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import io.netty.buffer.Unpooled;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class SchematicaSentinel extends SentinelModule implements Listener
{
    private final byte[] sentMessage;

    public SchematicaSentinel()
    {
        super("Schematica");
        val byteBuf = Unpooled.buffer();
        byteBuf.writeByte(0);

        /*
         * This array holds what features of schematica should be disabled.
         * SENDING 1 MEANS ALLOWING THE FEATURE -> NEGATION.
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

        MessageChannel.SCHEMATICA_CHANNEL.getChannel().ifPresent(schematicaChannel -> {
            if (user.getPlayer().getListeningPluginChannels().contains(schematicaChannel)) {
                detection(user.getPlayer());
                user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), schematicaChannel, sentMessage);
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

package de.photon.aacadditionpro.modules.sentinel;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
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

public class SchematicaSentinel extends SentinelModule implements Listener
{
    private static final MessageChannel SCHEMATICA_CHANNEL = MessageChannel.ofLegacy("schematica");

    private final ByteBuf sentMessage;

    public SchematicaSentinel(String restString)
    {
        super(restString);
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

        this.sentMessage = Unpooled.unmodifiableBuffer(byteBuf);
    }

    @EventHandler
    public void on(final PlayerJoinEvent event)
    {
        val user = User.getUser(event.getPlayer().getUniqueId());
        if (User.isUserInvalid(user, this)) return;

        user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), SCHEMATICA_CHANNEL.getChannel(), sentMessage.array());
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .addOutgoingMessageChannels(SCHEMATICA_CHANNEL)
                           .addAllowedServerVersions(ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS).build();
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message)
    {
        // Ignore.
    }
}

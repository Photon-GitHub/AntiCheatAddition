package de.photon.aacadditionproold.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.ServerVersion;
import de.photon.aacadditionproold.modules.ListenerModule;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.PluginMessageListenerModule;
import de.photon.aacadditionproold.modules.RestrictedServerVersion;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.UserManager;
import de.photon.aacadditionproold.util.pluginmessage.MessageChannel;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;

public class SchematicaControl extends ClientControlModule implements ListenerModule, PluginMessageListenerModule, RestrictedServerVersion
{
    @Getter
    private static final SchematicaControl instance = new SchematicaControl();

    private static final MessageChannel SCHEMATICA_CHANNEL = new MessageChannel("minecraft", "schematica", "schematica");

    /**
     * This array holds what features of schematica should be disabled.
     * Please note that the original plugin uses a positive layout (only allow what is set) and here we use a negative
     * layout (only disable what is set), which is why the values are inverted. <p></p>
     * <p>
     * Link to the original plugin: <a href="https://www.spigotmc.org/resources/schematicaplugin.14411/">https://www.spigotmc.org/resources/schematicaplugin.14411/</a>
     */
    private final boolean[] disable = {
            !AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.printer"),
            !AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.saveToFile"),
            !AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.load")
    };

    @EventHandler
    public void on(final PlayerJoinEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // Encoding the data
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream))
        {
            dataOutputStream.writeByte(0);

            for (final boolean b : disable) {
                dataOutputStream.writeBoolean(b);
            }

            user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(),
                                               SCHEMATICA_CHANNEL.getChannel(),
                                               Objects.requireNonNull(byteArrayOutputStream.toByteArray(), "Schematica plugin message is null"));
        } catch (final IOException e) {
            AACAdditionPro.getInstance().getLogger().log(Level.SEVERE, "Could not write the Schematica packet.", e);
        }
    }

    @Override
    public Set<MessageChannel> getOutgoingChannels()
    {
        return ImmutableSet.of(SCHEMATICA_CHANNEL);
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCHEMATICA_CONTROL;
    }

    @Override
    public Set<ServerVersion> getSupportedVersions()
    {
        return ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS;
    }
}

package de.photon.aacadditionpro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PluginMessageListenerModule;
import de.photon.aacadditionpro.modules.RestrictedServerVersion;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.pluginmessage.MessageChannel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

public class SchematicaControl extends ClientControlModule implements ListenerModule, PluginMessageListenerModule, RestrictedServerVersion
{
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
            e.printStackTrace();
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

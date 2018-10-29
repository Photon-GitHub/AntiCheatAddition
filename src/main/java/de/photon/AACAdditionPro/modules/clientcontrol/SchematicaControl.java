package de.photon.AACAdditionPro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ServerVersion;
import de.photon.AACAdditionPro.modules.ListenerModule;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PluginMessageListenerModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;

public class SchematicaControl extends ClientControlModule implements ListenerModule, PluginMessageListenerModule
{
    private static final String SCHEMATICA_CHANNEL = ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS.contains(ServerVersion.getActiveServerVersion()) ?
                                                     "schematica" :
                                                     "minecraft:schematica";

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
                                               SCHEMATICA_CHANNEL,
                                               Objects.requireNonNull(byteArrayOutputStream.toByteArray(), "Schematica plugin message is null"));
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {}

    @Override
    public Set<String> getLegacyPluginMessageChannels()
    {
        return ImmutableSet.of(SCHEMATICA_CHANNEL);
    }

    @Override
    public Set<String> getPluginMessageChannels()
    {
        return ImmutableSet.of(SCHEMATICA_CHANNEL);
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCHEMATICA_CONTROL;
    }
}

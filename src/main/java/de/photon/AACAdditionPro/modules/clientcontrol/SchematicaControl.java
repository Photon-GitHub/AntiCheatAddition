package de.photon.AACAdditionPro.modules.clientcontrol;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.AACAdditionPro;
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
import java.util.Set;

public class SchematicaControl extends ClientControlModule implements ListenerModule, PluginMessageListenerModule
{
    @SuppressWarnings("FieldCanBeLocal")
    private static final String SCHEMATICA_CHANNEL = "schematica";

    /**
     * This depicts what features of schematica are allowed
     * true means that the
     * <p>
     * [0] | printer <br>
     * [1] | saveToFile <br>
     * [2] | load
     */
    private final boolean[] features = new boolean[3];

    public SchematicaControl()
    {
        // Disable parts
        // True in config = Do disable
        features[0] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.printer");
        features[1] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.saveToFile");
        features[2] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getModuleType().getConfigString() + ".disable.load");
    }

    @EventHandler
    public void on(final PlayerJoinEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (User.isUserInvalid(user, this.getModuleType()))
        {
            return;
        }

        // Encoding the data
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        try
        {
            dataOutputStream.writeByte(0);
            for (final boolean b : features)
            {
                dataOutputStream.writeBoolean(b);
            }
        } catch (final IOException e)
        {
            e.printStackTrace();
        }

        // The message that is sent to disable the plugin
        final byte[] pluginMessage = byteArrayOutputStream.toByteArray();

        // TODO: Is this working? Maybe have a deeper look into Schematica.
        if (pluginMessage != null)
        {
            user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), SCHEMATICA_CHANNEL, pluginMessage);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {}

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

package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.checks.ClientControlCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;

public class SchematicaControl implements Listener, ClientControlCheck
{
    @SuppressWarnings("FieldCanBeLocal")
    private static final String SCHEMATICA_CHANNEL = "schematica";

    /**
     * This depicts what features of schematica are allowed
     * true means that the
     * <p>
     * [0] | printer <br>
     * [1] | save <br>
     * [2] | load
     */
    private final boolean[] features = new boolean[3];

    public SchematicaControl()
    {
        // Disable parts
        // True in config = Do disable
        features[0] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getAdditionHackType().getConfigString() + ".disable.printer");
        features[1] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getAdditionHackType().getConfigString() + ".disable.save");
        features[2] = AACAdditionPro.getInstance().getConfig().getBoolean(this.getAdditionHackType().getConfigString() + ".disable.load");
    }

    @EventHandler
    public void on(final PlayerJoinEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        if (AACAdditionProCheck.isUserInvalid(user)) {
            return;
        }

        // Encoding the data
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        final DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);

        try {
            dataOutputStream.writeByte(0);
            for (final boolean b : features) {
                dataOutputStream.writeBoolean(b);
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }

        // The message that is sent to disable the plugin
        final byte[] pluginMessage = byteArrayOutputStream.toByteArray();

        // TODO: Is this working? Maybe have a deeper look into Schematica.
        if (pluginMessage != null) {
            user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), SCHEMATICA_CHANNEL, pluginMessage);
        }
    }

    @Override
    public String[] getPluginMessageChannels()
    {
        return new String[]{
                SCHEMATICA_CHANNEL
        };
    }

    @Override
    public List<String> getCommandsOnDetection()
    {
        return null;
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.SCHEMATICA_CONTROL;
    }
}

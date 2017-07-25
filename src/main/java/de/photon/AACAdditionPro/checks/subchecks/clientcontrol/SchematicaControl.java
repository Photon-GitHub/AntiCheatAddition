package de.photon.AACAdditionPro.checks.subchecks.clientcontrol;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.ClientControlCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

public class SchematicaControl implements PluginMessageListener, Listener, ClientControlCheck
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

        if (user == null || user.isBypassed()) {
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

        // Player reflection
        final Class<? extends Player> classOfPlayer = event.getPlayer().getClass();

        // Error fix with fake players
        if (classOfPlayer.getSimpleName().equals("CraftPlayer")) {
            // Get the methods to register a new channel with reflection
            final Method addChannel;
            final Method removeChannel;

            try {

                // TODO: Probably wrong channels, make it work.
                addChannel = classOfPlayer.getDeclaredMethod("addChannel", String.class);
                removeChannel = classOfPlayer.getDeclaredMethod("removeChannel", String.class);

                addChannel.invoke(user.getPlayer(), SCHEMATICA_CHANNEL);
                user.getPlayer().sendPluginMessage(AACAdditionPro.getInstance(), SCHEMATICA_CHANNEL, pluginMessage);
                removeChannel.invoke(user.getPlayer(), SCHEMATICA_CHANNEL);

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                System.out.println("Could not send Schematica message");
                e.printStackTrace();
            }
        }
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

    @Override
    public void subEnable()
    {
        AACAdditionPro.getInstance().registerListener(this);
        AACAdditionPro.getInstance().getServer().getMessenger().registerOutgoingPluginChannel(AACAdditionPro.getInstance(), SCHEMATICA_CHANNEL);
    }

    @Override
    public void onPluginMessageReceived(final String s, final Player player, final byte[] bytes) {}
}

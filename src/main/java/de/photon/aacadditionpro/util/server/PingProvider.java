package de.photon.aacadditionpro.util.server;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.exception.UnknownMinecraftException;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.reflection.ClassReflect;
import de.photon.aacadditionpro.util.reflection.Reflect;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.bukkit.entity.Player;

import java.util.logging.Level;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PingProvider
{
    public static final int FAIL_PING = -1;
    private static final ClassReflect CRAFTPLAYER_CLASS_REFLECT = Reflect.fromOBC("entity.CraftPlayer");
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static final Pattern PING_PATTERN = Pattern.compile("=\\d+ms");

    /**
     * Reflects the real ping of a {@link Player} from the CraftPlayer class.
     *
     * @param player the {@link Player} whose ping should be returned.
     *
     * @return the ping of the player or 0 if an exception is thrown.
     */
    public static int getPing(final Player player)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC18:
            case MC19:
            case MC110:
            case MC111:
            case MC112:
            case MC113:
            case MC114:
            case MC115:
                val craftPlayer = CRAFTPLAYER_CLASS_REFLECT.method("getHandle").invoke(player);
                try {
                    return craftPlayer.getClass().getDeclaredField("ping").getInt(craftPlayer);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    AACAdditionPro.getInstance().getLogger().log(Level.WARNING, "Failed to retrieve ping of a player: ", e);
                    return 0;
                }
            case MC116:
                return player.getPing();
            default:
                throw new UnknownMinecraftException();
        }
    }

    /**
     * Tries to get the player ping via a ping command on the system.
     */
    public static long getEchoPing(final User user)
    {
        val received = user.getTimestampMap().at(TimestampKey.PINGSPOOF_RECEIVED_PACKET).getTime();
        return received < 0 ? -1 : received - user.getTimestampMap().at(TimestampKey.PINGSPOOF_SENT_PACKET).getTime();
    }
}

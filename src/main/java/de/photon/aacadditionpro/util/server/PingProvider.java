package de.photon.aacadditionpro.util.server;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.exception.UnknownMinecraftException;
import de.photon.aacadditionpro.util.reflection.ClassReflect;
import de.photon.aacadditionpro.util.reflection.Reflect;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.bukkit.entity.Player;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PingProvider
{
    public static final int FAIL_PING = -1;
    private static final ClassReflect CRAFTPLAYER_CLASS_REFLECT = Reflect.fromOBC("entity.CraftPlayer");
    private static final boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("win");
    private static final Pattern PING_PATTERN = Pattern.compile("\\d+ms");

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
     * Tries to get the player ping in different
     */
    public static int getEchoPing(Player player)
    {
        // Return FAIL_PING to prevent false positives and bugs.
        if (player == null || player.getAddress() == null) return FAIL_PING;

        try {
            // Windows has a different ping command.
            val processBuilder = new ProcessBuilder("ping", IS_WINDOWS ? "-n" : "-c", "1", player.getAddress().getAddress().getHostAddress());
            val process = processBuilder.start();

            try (val stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                // read the output from the command
                String s;
                while ((s = stdInput.readLine()) != null) {
                    val matcher = PING_PATTERN.matcher(s);
                    if (matcher.matches()) {
                        String found = matcher.group();
                        found = found.substring(1, found.length() - 2);
                        return Integer.parseInt(found);
                    }
                }
            }
        } catch (IOException e) {
            return FAIL_PING;
        }
    }
}

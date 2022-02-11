package de.photon.aacadditionpro.util.minecraft.ping;

import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.user.User;
import org.bukkit.entity.Player;

public interface PingProvider
{
    PingProvider INSTANCE = ServerVersion.containsActiveServerVersion(ServerVersion.MC115.getVersionsTo()) ? new LegacyPingProvider() : new ModernPingProvider();

    /**
     * Tries to get the player ping via a ping command on the system.
     *
     * @return the floating average of the ping of the player. If the player has just joined, 200ms are the default value for the ping.
     */
    default long getEchoPing(final User user)
    {
        return (long) user.getPingspoofPing().getAverage();
    }

    /**
     * Reflects the real ping of a {@link Player} from the CraftPlayer class or uses the .getPing() method on newer versions.
     *
     * @param player the {@link Player} whose ping should be returned.
     *
     * @return the ping of the player or 0 if an exception is thrown.
     */
    int getPing(final Player player);
}

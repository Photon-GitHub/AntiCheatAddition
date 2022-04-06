package de.photon.anticheataddition.util.minecraft.ping;

import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.user.User;
import org.bukkit.entity.Player;

public interface PingProvider
{
    PingProvider INSTANCE = ServerVersion.containsActive(ServerVersion.MC115.getSupVersionsTo()) ? new LegacyPingProvider() : new ModernPingProvider();

    /**
     * Tries to get the player ping via a ping command on the system.
     *
     * @return the floating average of the ping of the player. If the player has just joined, 200ms are the default value for the ping.
     */
    default long getEchoPing(User user)
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
    int getPing(Player player);

    /**
     * Checks if a player's ping is at most maxPing or the maxPing is negative.
     *
     * @param maxPing the maximum ping a player may have to make this method return true.
     *                A negative ping will always return true.
     */
    default boolean atMostMaxPing(Player player, int maxPing)
    {
        return maxPing < 0 || this.getPing(player) <= maxPing;
    }
}

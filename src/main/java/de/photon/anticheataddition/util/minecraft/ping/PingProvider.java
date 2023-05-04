package de.photon.anticheataddition.util.minecraft.ping;

import de.photon.anticheataddition.ServerVersion;
import org.bukkit.entity.Player;

public sealed interface PingProvider permits LegacyPingProvider, ModernPingProvider
{
    PingProvider INSTANCE = ServerVersion.MC115.activeIsEarlierOrEqual() ? new LegacyPingProvider() : new ModernPingProvider();

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

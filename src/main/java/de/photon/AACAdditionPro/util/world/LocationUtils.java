package de.photon.AACAdditionPro.util.world;

import org.bukkit.Location;

/**
 * Provides util methods regarding {@link Location}s.
 */
public final class LocationUtils
{
    /**
     * Gets the squared distance of two {@link Location}s, correctly handling cross-world requests.
     *
     * @return the squared distance of the two {@link Location}s or {@link Double#POSITIVE_INFINITY} if they are from different worlds.
     */
    public static double safeWorldDistanceSquared(final Location firstLocation, final Location secondLocation)
    {
        return firstLocation.getWorld().getUID().equals(secondLocation.getWorld().getUID()) ?
               firstLocation.distanceSquared(secondLocation) :
               Double.POSITIVE_INFINITY;
    }

    /**
     * Simple method to know if a {@link Location} is close to another {@link Location}
     *
     * @param firstLocation   the first {@link Location}
     * @param secondLocation  the second {@link Location}
     * @param squaredDistance the squared distance that must be at most between the two {@link Location}s to make this {@link java.lang.reflect.Method} return true.
     *
     * @return true if the {@link Location} are in range, false if not
     */
    public static boolean areLocationsInRange(final Location firstLocation, final Location secondLocation, final double squaredDistance)
    {
        return firstLocation.getWorld().getName().equals(secondLocation.getWorld().getName()) &&
               firstLocation.distanceSquared(secondLocation) <= squaredDistance;
    }
}

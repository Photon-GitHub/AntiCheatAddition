package de.photon.AACAdditionPro.util.world;

import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import org.bukkit.Location;

/**
 * Provides util methods regarding {@link Location}s.
 */
public final class LocationUtils
{
    /**
     * Simple method to know if a {@link Location} is close to another {@link Location}
     *
     * @param firstLocation  the first {@link Location}
     * @param secondLocation the second {@link Location}
     * @param x              how far should the {@link Location}s at most be apart on the x-Axis
     * @param y              how far should the {@link Location}s at most be apart on the y-Axis
     * @param z              how far should the {@link Location}s at most be apart on the z-Axis
     *
     * @return true if the {@link Location} are in range, false if not
     */
    public static boolean areLocationsInRange(final Location firstLocation, final Location secondLocation, final double x, final double y, final double z)
    {
        return firstLocation.getWorld().getName().equals(secondLocation.getWorld().getName()) &&
               MathUtils.roughlyEquals(firstLocation.getX(), secondLocation.getX(), x) &&
               MathUtils.roughlyEquals(firstLocation.getY(), secondLocation.getY(), y) &&
               MathUtils.roughlyEquals(firstLocation.getZ(), secondLocation.getZ(), z);
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

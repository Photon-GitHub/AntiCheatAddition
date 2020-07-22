package de.photon.aacadditionpro.util.world;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

/**
 * Provides util methods regarding {@link Location}s.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LocationUtils
{
    /**
     * Checks if two Entities are in the same world.
     */
    public static boolean inSameWorld(Entity entity1, Entity entity2)
    {
        return entity1.getWorld().getUID().equals(entity2.getWorld().getUID());
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
        return firstLocation.getWorld().getUID().equals(secondLocation.getWorld().getUID()) &&
               firstLocation.distanceSquared(secondLocation) <= squaredDistance;
    }
}

package de.photon.anticheataddition.util.minecraft.world;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.photon.anticheataddition.modules.additions.esp.Esp;
import de.photon.anticheataddition.util.mathematics.ResetLocation;
import de.photon.anticheataddition.util.mathematics.ResetVector;
import de.photon.anticheataddition.util.mathematics.TimeUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public final class OcclusionUtil
{

    // This cache reduces the required getBlock() calls.
    public static final LoadingCache<Location, Boolean> BLOCK_CACHE = CacheBuilder.newBuilder()
                                                                                  // About 32000 locations can be cached at most.
                                                                                  .maximumSize(1L << 14)
                                                                                  // Use this expireAfterWrite method as the other is not compatible with 1.8.8.
                                                                                  .expireAfterWrite(TimeUtil.toMillis(Esp.ESP_INTERVAL_TICKS), TimeUnit.MILLISECONDS).build(new CacheLoader<>()
            {
                @Override
                public @NotNull Boolean load(@NotNull Location key)
                {
                    final Block block = key.getBlock();
                    return block != null && !block.isEmpty() && MaterialUtil.isReallyOccluding(block.getType());
                }
            });

    /**
     * Get to know where the {@link Vector} intersects with a {@link org.bukkit.block.Block}.
     * Non-Occluding {@link Block}s as defined in {@link MaterialUtil#isReallyOccluding(Material)} are ignored.
     *
     * @param start     the starting {@link Location}
     * @param direction the {@link Vector} which should be checked
     *
     * @return The length when the {@link Vector} intersects or 0 if no intersection was found
     */
    public static double getDistanceToFirstIntersectionWithBlock(final Location start, final Vector direction, final int distance)
    {
        Preconditions.checkNotNull(start.getWorld(), "RayTrace: Unknown start world.");

        if (distance >= 1) {
            try {
                final var blockIterator = new BlockIterator(start.getWorld(), start.toVector(), direction, 0, distance);
                Block block;
                while (blockIterator.hasNext()) {
                    block = blockIterator.next();
                    // Use the middle location of the Block instead of the simple location.
                    if (OcclusionUtil.isOccludingLocation(block.getLocation())) return block.getLocation().clone().add(0.5, 0.5, 0.5).distance(start);
                }
            } catch (IllegalStateException exception) {
                // Just in case the start block could not be found for some reason or a chunk is loaded async.
                return 0;
            }
        }
        return 0;
    }

    public static boolean isOccludingLocation(final Location location)
    {
        return BLOCK_CACHE.getUnchecked(location);
    }

    public static boolean isRayOccluded(final Location from, final Location to)
    {
        return isRayOccluded(from, to.toVector().subtract(from.toVector()), to);
    }

    public static boolean isRayOccluded(final Location from, final Vector between, final Location to)
    {
        if (!WorldUtil.INSTANCE.areChunksLoadedBetweenLocations(from, to)) return false;

        final var resetFrom = new ResetLocation(from);
        final var normalBetween = new ResetVector(between.normalize());
        for (double d : heuristicScalars(from.distance(to))) {
            // An occluding block exists.
            if (isOccludingLocation(resetFrom.add(normalBetween.multiply(d)))) return true;

            resetFrom.resetToBase();
            normalBetween.resetToBase();
        }
        return false;
    }

    public static double[] heuristicScalars(final double distance)
    {
        if (distance <= 2) return new double[]{1};
        if (distance <= 3) return new double[]{1, distance - 1};
        if (distance <= 5) return new double[]{1, 1.5, 2, distance - 2, distance - 1.5, distance - 1};
        if (distance <= 10) return new double[]{1, 1.5, 2, 2.5, distance / 2, distance - 2.5, distance - 2, distance - 1.5, distance - 1};
        if (distance <= 25) return new double[]{1, 1.5, 2, 2.5, distance / 4, distance / 2, 3 * distance / 4, distance - 2.5, distance - 2, distance - 1.5, distance - 1};
        return new double[]{1, 1.5, 2, 2.5, distance / 4, distance / 3, distance / 2, 2 * distance / 3, 3 * distance / 4, distance - 2.5, distance - 2, distance - 1.5, distance - 1};
    }
}

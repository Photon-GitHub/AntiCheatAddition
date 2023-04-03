package de.photon.anticheataddition.modules.additions.esp;

import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import de.photon.anticheataddition.util.mathematics.ResetLocation;
import de.photon.anticheataddition.util.mathematics.ResetVector;
import de.photon.anticheataddition.util.mathematics.TimeUtil;
import de.photon.anticheataddition.util.minecraft.world.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public interface CanSee
{
    CanSee INSTANCE = Esp.ESP_INSTANCE.loadBoolean(".calculate_third_person_modes", false) ? new ThirdPersonCameraCanSee() : new SingleCameraCanSee();

    // This cache reduces the required getBlock() calls.
    LoadingCache<Location, Boolean> BLOCK_CACHE = CacheBuilder.newBuilder()
                                                              // About 32000 locations can be cached at most.
                                                              .maximumSize(1L << 14)
                                                              // Use this expireAfterWrite method as the other is not compatible with 1.8.8.
                                                              .expireAfterWrite(TimeUtil.toMillis(Esp.ESP_INTERVAL_TICKS), TimeUnit.MILLISECONDS)
                                                              .build(new CacheLoader<>()
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
    static double getDistanceToFirstIntersectionWithBlock(final Location start, final Vector direction)
    {
        Preconditions.checkNotNull(start.getWorld(), "RayTrace: Unknown start world.");
        final int length = (int) direction.length();

        if (length >= 1) {
            try {
                final var blockIterator = new BlockIterator(start.getWorld(), start.toVector(), direction, 0, length);
                Block block;
                while (blockIterator.hasNext()) {
                    block = blockIterator.next();
                    // Account for a Spigot bug: BARRIER and MOB_SPAWNER are not occluding blocks
                    // Use the middle location of the Block instead of the simple location.
                    if (MaterialUtil.isReallyOccluding(block.getType())) return block.getLocation().clone().add(0.5, 0.5, 0.5).distance(start);
                }
            } catch (IllegalStateException exception) {
                // Just in case the start block could not be found for some reason or a chunk is loaded async.
                return 0;
            }
        }
        return 0;
    }

    /**
     * This uses the heuristic scalars as defined in {@link #heuristicScalars(double)} to check if one can see the "to" {@link Location} from the "from" {@link Location}.
     *
     * @return <code>true</code> if no occluding blocks are found at the heuristic scalars, so one MAY see the "to" location.<br></br>
     * <code>false</code> if an occluding block has been found. In this case one cannot see the "to" location.
     */
    static boolean canSeeHeuristic(Location from, Vector between, Location to)
    {
        final var resetFrom = new ResetLocation(from);
        final var normalBetween = new ResetVector(between.normalize());
        for (double d : heuristicScalars(from.distance(to))) {
            // An occluding block exists.
            if (Boolean.TRUE.equals(BLOCK_CACHE.getUnchecked(resetFrom.add(normalBetween.multiply(d))))) return false;

            resetFrom.resetToBase();
            normalBetween.resetToBase();
        }
        return true;
    }

    private static double[] heuristicScalars(double distance)
    {
        if (distance <= 2) return new double[]{1};
        if (distance <= 3) return new double[]{1, distance - 1};
        if (distance <= 5) return new double[]{1, 1.5, 2, distance - 2, distance - 1.5, distance - 1};
        if (distance <= 10) return new double[]{1, 1.5, 2, 2.5, distance / 2, distance - 2.5, distance - 2, distance - 1.5, distance - 1};
        if (distance <= 25) return new double[]{1, 1.5, 2, 2.5, distance / 4, distance / 2, 3 * distance / 4, distance - 2.5, distance - 2, distance - 1.5, distance - 1};
        return new double[]{1, 1.5, 2, 2.5, distance / 4, distance / 3, distance / 2, 2 * distance / 3, 3 * distance / 4, distance - 2.5, distance - 2, distance - 1.5, distance - 1};
    }

    boolean canSee(Player observer, Player watched);
}

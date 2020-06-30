package de.photon.aacadditionpro.modules.checks.esp;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.util.world.BlockUtils;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class EspUtil
{
    // The camera offset for 3rd person
    private static final boolean RAY_TRACING = AACAdditionPro.getInstance().getConfig().getBoolean(ModuleType.ESP.getConfigString() + ".ray_tracing", false);

    private EspUtil() {}

    /**
     * Get to know where the {@link Vector} intersects with a {@link org.bukkit.block.Block}.
     * Non-Occluding {@link Block}s as defined in {@link BlockUtils#isReallyOccluding(Material)} are ignored.
     *
     * @param start     the starting {@link Location}
     * @param direction the {@link Vector} which should be checked
     *
     * @return The length when the {@link Vector} intersects or 0 if no intersection was found
     */
    public static double getDistanceToFirstIntersectionWithBlock(final Location start, final Vector direction)
    {
        final int length = (int) Math.floor(direction.length());
        Preconditions.checkNotNull(start.getWorld(), "RayTrace: Unknown start world.");

        if (length >= 1) {
            if (RAY_TRACING) {
                RayTraceResult result = start.getWorld().rayTraceBlocks(start, direction, length, FluidCollisionMode.NEVER, true);
                // Hit nothing or the other player
                if (result == null || result.getHitBlock() == null) {
                    return 0;
                }

                return start.toVector().distance(result.getHitPosition());
            } else {
                try {
                    final BlockIterator blockIterator = new BlockIterator(start.getWorld(), start.toVector(), direction, 0, length);
                    Block block;
                    while (blockIterator.hasNext()) {
                        block = blockIterator.next();
                        // Account for a Spigot bug: BARRIER and MOB_SPAWNER are not occluding blocks
                        if (BlockUtils.isReallyOccluding(block.getType())) {
                            // Use the middle location of the Block instead of the simple location.
                            return block.getLocation().clone().add(0.5, 0.5, 0.5).distance(start);
                        }
                    }
                } catch (IllegalStateException exception) {
                    // Just in case the start block could not be found for some reason or a chunk is loaded async.
                    return 0;
                }
            }
        }
        return 0;
    }
}
package de.photon.AACAdditionPro.util.mathematics;

import de.photon.AACAdditionPro.util.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public final class VectorUtils
{
    /**
     * Get to know where the {@link Vector} intersects with a {@link org.bukkit.block.Block}.
     * Non-Solid {@link org.bukkit.block.Block}s are ignored.
     *
     * @param start the starting {@link Location}
     * @param a     the {@link Vector} which should be checked
     *
     * @return The length when the {@link Vector} intersects or 0 if no intersection was found
     */
    public static double getFirstVectorIntersectionWithBlock(final Location start, final Vector a)
    {
        final int length = (int) Math.floor(a.length());
        if (length >= 1) {
            try {
                final BlockIterator blockIterator = new BlockIterator(start.getWorld(), start.toVector(), a, 0, length);
                while (blockIterator.hasNext()) {
                    final Block block = blockIterator.next();
                    // Account for a Spigot bug: BARRIER and MOB_SPAWNER are not occluding blocks
                    if (BlockUtils.isReallyOccluding(block.getType())) {
                        return block.getLocation().distance(start);
                    }
                }
            } catch (final IllegalStateException ignored) {
            }
        }
        return 0;
    }

    /**
     * Checks if the {@link Vector} a intersects with an occluding and solid {@link Block} after length.
     *
     * @param start  the {@link Location} where the {@link Vector} a is starting
     * @param a      the {@link Vector} which should be checked
     * @param length the {@link Block}-check takes place at the location start + a.normalize().multiply(length)
     */
    public static boolean vectorIntersectsWithBlockAt(final Location start, final Vector a, final double length)
    {
        final Material type = start.clone().add(a.clone().normalize().multiply(length)).getBlock().getType();
        return BlockUtils.isReallyOccluding(type) && type.isSolid();
    }
}
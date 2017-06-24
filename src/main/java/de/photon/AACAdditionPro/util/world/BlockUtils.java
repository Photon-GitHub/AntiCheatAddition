package de.photon.AACAdditionPro.util.world;

import com.google.common.collect.ImmutableList;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.List;

public final class BlockUtils
{

    private static final List<BlockFace> horizontalFaces = ImmutableList.of(
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.EAST);
    private static final List<BlockFace> allFaces = ImmutableList.of(
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.EAST);

    /**
     * This can be used to know if the {@link Block}s are next to each other.
     *
     * @param a              the first {@link Block}
     * @param b              the second {@link Block}
     * @param onlyHorizontal whether only the horizontal {@link BlockFace}s should be checked for the second {@link Block} or all {@link BlockFace}s (horizontal + vertical)
     *
     * @return true if the {@link Block}s are next to each other, or false if they are not
     */
    public static boolean isNext(final Block a, final Block b, final boolean onlyHorizontal)
    {
        if (!a.getWorld().equals(b.getWorld())) {
            return false;
        }

        for (final BlockFace face : onlyHorizontal ?
                                    horizontalFaces :
                                    allFaces) {
            if (a.getRelative(face).equals(b)) {
                return true;
            }
        }
        return false;
    }

    /**
     * This counts the {@link Block}s around the given block if they are not air/empty.
     *
     * @param block          the block that faces should be checked for other {@link Block}s
     * @param onlyHorizontal whether only the {@link Block}s should be counted that are horizontal around the block or all {@link Block}s (horizontal + vertical)
     *
     * @return the amount of {@link Block}s which were counted
     */
    public static byte blocksAround(final Block block, final boolean onlyHorizontal)
    {
        byte count = 0;
        for (final BlockFace f : onlyHorizontal ?
                                 horizontalFaces :
                                 allFaces) {
            if (!block.getRelative(f).isEmpty()) {
                count++;
            }
        }
        return count;
    }
}

package de.photon.AACAdditionPro.util.world;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.ServerVersion;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class BlockUtils
{
    public static final Set<Material> LIQUIDS;
    public static final Set<Material> CONTAINERS;

    static
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                CONTAINERS = ImmutableSet.of(Material.CHEST,
                                             Material.TRAPPED_CHEST,
                                             Material.ENDER_CHEST,
                                             Material.ANVIL,
                                             Material.FURNACE,
                                             Material.DISPENSER,
                                             Material.DROPPER,
                                             Material.BREWING_STAND);
                LIQUIDS = ImmutableSet.of(Material.WATER,
                                          Material.LAVA,
                                          Material.getMaterial("STATIONARY_WATER"),
                                          Material.getMaterial("STATIONARY_LAVA"));
                break;
            case MC111:
            case MC112:
                CONTAINERS = ImmutableSet.of(Material.CHEST,
                                             Material.TRAPPED_CHEST,
                                             Material.ENDER_CHEST,
                                             Material.ANVIL,
                                             Material.FURNACE,
                                             Material.DISPENSER,
                                             Material.DROPPER,
                                             Material.BREWING_STAND,
                                             Material.BLACK_SHULKER_BOX,
                                             Material.BROWN_SHULKER_BOX,
                                             Material.BLUE_SHULKER_BOX,
                                             Material.CYAN_SHULKER_BOX,
                                             Material.GRAY_SHULKER_BOX,
                                             Material.GREEN_SHULKER_BOX,
                                             Material.LIGHT_BLUE_SHULKER_BOX,
                                             Material.LIME_SHULKER_BOX,
                                             Material.MAGENTA_SHULKER_BOX,
                                             Material.ORANGE_SHULKER_BOX,
                                             Material.PINK_SHULKER_BOX,
                                             Material.PURPLE_SHULKER_BOX,
                                             Material.RED_SHULKER_BOX,
                                             Material.getMaterial("SILVER_SHULKER_BOX"),
                                             Material.WHITE_SHULKER_BOX,
                                             Material.YELLOW_SHULKER_BOX);
                LIQUIDS = ImmutableSet.of(Material.WATER,
                                          Material.LAVA,
                                          Material.getMaterial("STATIONARY_WATER"),
                                          Material.getMaterial("STATIONARY_LAVA"));
                break;
            case MC113:
                CONTAINERS = ImmutableSet.of(Material.CHEST,
                                             Material.TRAPPED_CHEST,
                                             Material.ENDER_CHEST,
                                             Material.ANVIL,
                                             Material.FURNACE,
                                             Material.DISPENSER,
                                             Material.DROPPER,
                                             Material.BREWING_STAND,
                                             Material.SHULKER_BOX,
                                             Material.BLACK_SHULKER_BOX,
                                             Material.BROWN_SHULKER_BOX,
                                             Material.BLUE_SHULKER_BOX,
                                             Material.CYAN_SHULKER_BOX,
                                             Material.GRAY_SHULKER_BOX,
                                             Material.GREEN_SHULKER_BOX,
                                             Material.LIGHT_BLUE_SHULKER_BOX,
                                             Material.LIME_SHULKER_BOX,
                                             Material.MAGENTA_SHULKER_BOX,
                                             Material.ORANGE_SHULKER_BOX,
                                             Material.PINK_SHULKER_BOX,
                                             Material.PURPLE_SHULKER_BOX,
                                             Material.RED_SHULKER_BOX,
                                             Material.WHITE_SHULKER_BOX,
                                             Material.YELLOW_SHULKER_BOX);

                LIQUIDS = ImmutableSet.of(Material.WATER,
                                          Material.LAVA);
                break;
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }

    public static final Set<BlockFace> HORIZONTAL_FACES = ImmutableSet.of(
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.EAST);

    public static final Set<BlockFace> ALL_FACES = ImmutableSet.of(
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
        // Same world
        return a.getWorld().getUID().equals(b.getWorld().getUID())
               // Next to each other.
               && (onlyHorizontal ?
                   HORIZONTAL_FACES.contains(a.getFace(b)) :
                   ALL_FACES.contains(a.getFace(b)));
    }

    /**
     * This counts the {@link Block}s around the given block if they are not air/empty.
     *
     * @param block          the block that faces should be checked for other {@link Block}s
     * @param onlyHorizontal whether only the {@link Block}s should be counted that are horizontal around the block or all {@link Block}s (horizontal + vertical)
     *
     * @return the amount of {@link Block}s which were counted
     */
    public static byte countBlocksAround(final Block block, final boolean onlyHorizontal)
    {
        byte count = 0;
        for (final BlockFace f : onlyHorizontal ?
                                 HORIZONTAL_FACES :
                                 ALL_FACES)
        {
            if (!block.getRelative(f).isEmpty())
            {
                count++;
            }
        }
        return count;
    }

    /**
     * This gets the {@link Block}s around the given block if they are not air/empty.
     *
     * @param block          the block that faces should be checked for other {@link Block}s
     * @param onlyHorizontal whether only the {@link Block}s should be counted that are horizontal around the block or all {@link Block}s (horizontal + vertical)
     *
     * @return a {@link List} of all {@link Block}s which were found.
     */
    public static List<Block> getBlocksAround(final Block block, final boolean onlyHorizontal)
    {
        final List<Block> blocks = new ArrayList<>(onlyHorizontal ? 4 : 6);
        for (final BlockFace face : onlyHorizontal ?
                                    HORIZONTAL_FACES :
                                    ALL_FACES)
        {
            final Block relative = block.getRelative(face);
            if (!relative.isEmpty())
            {
                blocks.add(relative);
            }
        }
        return blocks;
    }

    /**
     * Fix for Spigot's broken occluding method.
     */
    public static boolean isReallyOccluding(Material material)
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
            case MC111:
            case MC112:
                return material != Material.BARRIER && material != Material.getMaterial("MOB_SPAWNER") && material.isOccluding();
            case MC113:
                return material != Material.BARRIER && material != Material.SPAWNER && material.isOccluding();
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
    }
}

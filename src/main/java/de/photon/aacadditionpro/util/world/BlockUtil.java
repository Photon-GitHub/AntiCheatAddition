package de.photon.aacadditionpro.util.world;

import com.google.common.collect.Sets;
import de.photon.aacadditionproold.ServerVersion;
import de.photon.aacadditionproold.util.exceptions.UnknownMinecraftVersion;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BlockUtil
{
    public static final Set<BlockFace> HORIZONTAL_FACES = Sets.immutableEnumSet(
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.EAST);
    public static final Set<BlockFace> ALL_FACES = Sets.immutableEnumSet(
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.EAST);

    /**
     * This can be used to know if the {@link Block}s are next to each other.
     *
     * @param a     the first {@link Block}
     * @param b     the second {@link Block}
     * @param faces the {@link BlockFace}s which shall be checked.
     *
     * @return true if the {@link Block}s are next to each other, or false if they are not
     */
    public static boolean isNext(@NotNull final Block a, @NotNull final Block b, @NotNull final Set<BlockFace> faces)
    {
        // Same world
        return a.getWorld().getUID().equals(b.getWorld().getUID())
               // Next to each other.
               && faces.contains(a.getFace(b));
    }

    /**
     * This counts the {@link Block}s around the given block if they are not air/empty.
     *
     * @param block   the block that faces should be checked for other {@link Block}s
     * @param faces   the {@link Set} containing all {@link BlockFace}s which shall be included.
     * @param ignored the {@link Set} of {@link Material}s which shall be ignored while counting.
     *                Empty blocks are automatically ignored.
     *
     * @return the amount of {@link Block}s which were counted
     */
    public static byte countBlocksAround(@NotNull final Block block, final Set<BlockFace> faces, @NotNull final Set<Material> ignored)
    {
        byte count = 0;
        Block relative;
        for (final BlockFace f : faces) {
            relative = block.getRelative(f);
            if (!relative.isEmpty() && !ignored.contains(relative.getType())) ++count;
        }
        return count;
    }

    /**
     * This gets the {@link Block}s around the given block if they are not air/empty.
     *
     * @param block the block that faces should be checked for other {@link Block}s
     * @param faces the {@link Set} containing all {@link BlockFace}s which shall be included.
     *
     * @return a {@link List} of all {@link Block}s which were found.
     */
    public static List<Block> getBlocksAround(final Block block, final Set<BlockFace> faces)
    {
        final List<Block> blocks = new ArrayList<>(6);
        Block relative;
        for (final BlockFace face : faces) {
            relative = block.getRelative(face);
            if (!relative.isEmpty()) blocks.add(relative);
        }
        return blocks;
    }

    /**
     * This determines if an {@link org.bukkit.inventory.InventoryView} can be opened by interacting with the {@link Block}.
     */
    public static boolean isInventoryOpenable(@NotNull final Block block)
    {
        // The block actually is holding an inventory (and therefore is not e.g. dirt)
        if (block.getState() instanceof InventoryHolder) {
            // Additional checks for cats and occluding blocks necessary?
            if (MaterialUtil.FREE_SPACE_CONTAINERS.contains(block.getType())) {
                final Block aboveBlock = block.getRelative(BlockFace.UP);
                final Location checkForCatLocation = aboveBlock.getLocation().add(0.5, 0.5, 0.5);

                switch (ServerVersion.getActiveServerVersion()) {
                    case MC188:
                    case MC112:
                        // 1.8.8 and 1.12 doesn't provide isPassable.
                        // Make sure that the block above is not obstructed by blocks
                        // Cannot check for cats on 1.8 and 1.12 as the server version doesn't provide the newer methods.
                        return MaterialUtil.FREE_SPACE_CONTAINERS_ALLOWED_MATERIALS.contains(aboveBlock.getType());
                    case MC113:
                        // Make sure that the block above is not obstructed by blocks
                        return (aboveBlock.isPassable() || MaterialUtil.FREE_SPACE_CONTAINERS_ALLOWED_MATERIALS.contains(aboveBlock.getType()))
                               // Make sure that the block above is not obstructed by cats
                               && aboveBlock.getWorld().getNearbyEntities(checkForCatLocation, 0.5, 0.5, 0.5, EntityUtil.ofType(EntityType.OCELOT)).isEmpty();
                    case MC114:
                    case MC115:
                    case MC116:
                        // Make sure that the block above is not obstructed by blocks
                        return (aboveBlock.isPassable() || MaterialUtil.FREE_SPACE_CONTAINERS_ALLOWED_MATERIALS.contains(aboveBlock.getType()))
                               // Make sure that the block above is not obstructed by cats
                               && aboveBlock.getWorld().getNearbyEntities(checkForCatLocation, 0.5, 0.5, 0.5, EntityUtil.ofType(EntityType.CAT)).isEmpty();
                    default:
                        throw new UnknownMinecraftVersion();
                }
            }
            return true;
        }
        return false;
    }
}

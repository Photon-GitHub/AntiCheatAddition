package de.photon.aacadditionpro.util.minecraft.world;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import de.photon.aacadditionpro.util.mathematics.Hitbox;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Contract;

import java.util.List;
import java.util.Set;

public interface WorldUtil
{
    WorldUtil INSTANCE = new ModernWorldUtil();

    /**
     * Only the {@link BlockFace}s that are directly adjacent to a {@link Block} on the same y-level.
     */
    Set<BlockFace> HORIZONTAL_FACES = Sets.immutableEnumSet(BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST);

    /**
     * The {@link BlockFace}s that are directly adjacent to a {@link Block} on the same y-level as well as the {@link Block}s directly below and up.
     */
    Set<BlockFace> ALL_FACES = Sets.immutableEnumSet(BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH, BlockFace.SOUTH, BlockFace.WEST, BlockFace.EAST);

    /**
     * Checks whether two {@link Block}s are next to each other defined by the given {@link BlockFace}s.
     *
     * @param a     the first {@link Block}
     * @param b     the second {@link Block}
     * @param faces the {@link BlockFace}s which shall be checked.
     *
     * @return true if the {@link Block}s are next to each other, or false if they are not
     */
    @Contract("null, _, _ -> fail; _, null, _ -> fail; _, _, null -> fail")
    boolean isNext(final Block a, final Block b, final Set<BlockFace> faces);

    /**
     * This counts the {@link Block}s around the given block if they are not air/empty.
     *
     * @param block   the block that faces should be checked for other {@link Block}s
     * @param faces   the {@link Set} containing all {@link BlockFace}s which shall be included.
     * @param ignored the {@link Set} of {@link Material}s which shall be ignored.
     *                Empty blocks are automatically ignored.
     *
     * @return the amount of {@link Block}s which were counted
     */
    @Contract("null, _, _ -> fail; _, null, _ -> fail; _, _, null -> fail")
    long countBlocksAround(final Block block, final Set<BlockFace> faces, final Set<Material> ignored);


    /**
     * This gets the {@link Block}s around the given block if they are not air/empty.
     *
     * @param block   the block that faces should be checked for other {@link Block}s
     * @param faces   the {@link Set} containing all {@link BlockFace}s which shall be included.
     * @param ignored the {@link Set} of {@link Material}s which shall be ignored.
     *                Empty blocks are automatically ignored.
     *
     * @return an immmutable {@link List} of {@link Block}s around the {@link Block}
     */
    @Contract("null, _, _ -> fail; _, null, _ -> fail; _, _, null -> fail")
    List<Block> getBlocksAround(final Block block, final Set<BlockFace> faces, final Set<Material> ignored);

    /**
     * Checks if two entities are in the same {@link World}.
     */
    @Contract("null, _ -> fail; _, null -> fail")
    default boolean inSameWorld(final Entity entity1, final Entity entity2)
    {
        Preconditions.checkNotNull(entity1, "Tried to check world of null entity 1.");
        Preconditions.checkNotNull(entity2, "Tried to check world of null entity 2.");
        return inSameWorld(entity1.getLocation(), entity2.getLocation());
    }

    /**
     * Checks if two {@link Location}s are in the same {@link World}.
     */
    @Contract("null, _ -> fail; _, null -> fail")
    boolean inSameWorld(final Location locationOne, final Location locationTwo);

    /**
     * Checks if the chunk at a certain {@link Location} is loaded.
     *
     * @return True if the chunk is loaded, else false.
     */
    @Contract("null -> fail")
    boolean isChunkLoaded(final Location location);

    /**
     * Checks if the chunk at certain coordinates is loaded.
     *
     * @param world  the {@link World} to check for the chunk
     * @param blockX the x - coordinate of a {@link Block} in that {@link World}
     * @param blockZ the z - coordinate of a {@link Block} in that {@link World}
     *
     * @return True if the chunk is loaded, else false.
     */
    @Contract("null, _ , _ -> fail")
    boolean isChunkLoaded(final World world, final int blockX, final int blockZ);

    /**
     * Checks if the chunks between two locations are loaded without trying to load them.
     * This method should be used to see if a calculation is safe for async usage.
     */
    @Contract("null, _ -> fail; _, null -> fail")
    boolean areChunksLoadedBetweenLocations(final Location one, final Location two);

    /**
     * Simple method to know if a {@link Location} is close to another {@link Location}
     *
     * @param firstLocation  the first {@link Location}
     * @param secondLocation the second {@link Location}
     * @param distance       the distance that must be at most between the two {@link Location}s to return true.
     *
     * @return true if the {@link Location} are in range, false if not
     */
    @Contract("null, _, _ -> fail; _, null, _ -> fail")
    boolean areLocationsInRange(final Location firstLocation, final Location secondLocation, final double distance);

    /**
     * This determines if an inventory can be opened by interacting with the {@link Block}.
     * This might fail due to various reasons, e.g. obstruction of the {@link Block} by cats, other {@link Block}s, etc.
     */
    @Contract("null -> fail")
    boolean isInventoryOpenable(final Block block);

    /**
     * Gets all {@link LivingEntity}s around an {@link Entity}
     *
     * @param entity the entity from which the distance is measured
     * @param hitbox the {@link Hitbox} of the entity
     * @param offset additional distance from the hitbox in all directions
     *
     * @return a {@link List} of {@link LivingEntity}s which are in range.
     */
    @Contract("null, _, _ -> fail; _, null, _ -> fail")
    default List<LivingEntity> getLivingEntitiesAroundEntity(final Entity entity, final Hitbox hitbox, final double offset)
    {
        Preconditions.checkNotNull(hitbox, "Tried to get living entities in null hitbox.");
        return getLivingEntitiesAroundEntity(entity, hitbox.getOffsetX() + offset, hitbox.getHeight() + offset, hitbox.getOffsetZ() + offset);
    }

    /**
     * Gets all {@link LivingEntity}s around an {@link Entity}
     *
     * @param entity the entity from which the distance is measured
     * @param x      the maximum x-distance between the initialPlayer and the checked {@link Player} to add the checked {@link Player} to the {@link List}.
     * @param y      the maximum y-distance between the initialPlayer and the checked {@link Player} to add the checked {@link Player} to the {@link List}.
     * @param z      the maximum z-distance between the initialPlayer and the checked {@link Player} to add the checked {@link Player} to the {@link List}.
     *
     * @return a {@link List} of {@link LivingEntity}s which are in range, excluding the given entity.
     */
    @Contract("null, _, _, _ -> fail")
    List<LivingEntity> getLivingEntitiesAroundEntity(final Entity entity, final double x, final double y, final double z);
}

package de.photon.anticheataddition.util.minecraft.world;

import com.google.common.base.Preconditions;
import com.google.common.math.DoubleMath;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.InventoryHolder;

import java.math.RoundingMode;
import java.util.List;
import java.util.Set;

final class ModernWorldUtil implements WorldUtil
{

    // Field for the polymorphic checker
    private final InventoryOpenableChecker inventoryChecker;

    // Constructor to initialize the checker based on server version
    ModernWorldUtil() {
        this.inventoryChecker = ServerVersion.MC112.activeIsEarlierOrEqual() ?
                new Pre112InventoryChecker() : new Post112InventoryChecker();
    }

    // Nested interface for inventory openable checking
    private interface InventoryOpenableChecker {
        boolean canOpenInventory(Block block, Block aboveBlock);
    }

    // Nested class for pre-1.12 versions
    private static class Pre112InventoryChecker implements InventoryOpenableChecker {
        @Override
        public boolean canOpenInventory(Block block, Block aboveBlock) {
            return !aboveBlock.getType().isOccluding();
        }
    }

    // Nested class for post-1.12 versions
    private static class Post112InventoryChecker implements InventoryOpenableChecker {
        @Override
        public boolean canOpenInventory(Block block, Block aboveBlock) {
            return !aboveBlock.getType().isOccluding() &&
                    aboveBlock.getWorld().getNearbyEntities(
                            aboveBlock.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5,
                            entity -> switch (entity.getType()) {
                                case CAT, OCELOT -> true;
                                default -> false;
                            }
                    ).isEmpty();
        }
    }

    @Override
    public boolean isNext(Block a, Block b, Set<BlockFace> faces)
    {
        Preconditions.checkNotNull(a, "Tried to check whether null block is next to another block.");
        Preconditions.checkNotNull(b, "Tried to check whether null block is next to another block.");
        Preconditions.checkNotNull(faces, "Tried to check whether block is next to another block at null faces.");
        return a.getWorld().getUID().equals(b.getWorld().getUID()) && faces.contains(a.getFace(b));
    }

    @Override
    public long countBlocksAround(Block block, Set<BlockFace> faces, Set<Material> ignored)
    {
        Preconditions.checkNotNull(block, "Tried to count surrounding blocks of null block.");
        Preconditions.checkNotNull(faces, "Tried to count surrounding blocks of block at null faces.");
        Preconditions.checkNotNull(faces, "Tried to count surrounding blocks of block will null ignore (use empty set).");

        return faces.stream()
                    .map(block::getRelative)
                    // Actual block there, not air.
                    .filter(b -> !b.isEmpty())
                    // Ignored materials.
                    .filter(b -> !ignored.contains(b.getType()))
                    .count();
    }

    @Override
    public List<Block> getBlocksAround(Block block, Set<BlockFace> faces, Set<Material> ignored)
    {
        Preconditions.checkNotNull(block, "Tried to get surrounding blocks of null block.");
        Preconditions.checkNotNull(faces, "Tried to get surrounding blocks of block at null faces.");
        Preconditions.checkNotNull(faces, "Tried to get surrounding blocks of block will null ignore (use empty set).");

        return faces.stream()
                    .map(block::getRelative)
                    // Actual block there, not air.
                    .filter(b -> !b.isEmpty())
                    // Ignored materials.
                    .filter(b -> !ignored.contains(b.getType()))
                    .toList();
    }

    @Override
    public boolean inSameWorld(Location locationOne, Location locationTwo)
    {
        Preconditions.checkNotNull(locationOne, "Tried to check world state of null location 1.");
        Preconditions.checkNotNull(locationTwo, "Tried to check world state of null location 2.");
        return Preconditions.checkNotNull(locationOne.getWorld(), "NULL world in same world comparison (one)").getUID()
                            .equals(Preconditions.checkNotNull(locationTwo.getWorld(), "NULL world in same world comparison (two)").getUID());
    }

    @Override
    public boolean isChunkLoaded(Location location)
    {
        Preconditions.checkNotNull(location, "Tried to check chunks in null location.");
        Preconditions.checkNotNull(location.getWorld(), "Tried to check chunks null world of a location.");
        return isChunkLoaded(location.getWorld(), location.getBlockX(), location.getBlockZ());
    }

    @Override
    public boolean isChunkLoaded(World world, int blockX, int blockZ)
    {
        Preconditions.checkNotNull(world, "Tried to check chunks in null world.");
        return world.isChunkLoaded(blockX >> 4, blockZ >> 4);
    }

    private static int toChunkCoordinate(double coordinate)
    {
        return DoubleMath.roundToInt(coordinate, RoundingMode.FLOOR) >> 4;
    }

    @Override
    public boolean areChunksLoadedBetweenLocations(Location one, Location two)
    {
        Preconditions.checkNotNull(one, "Tried to check chunks in null location one.");
        Preconditions.checkNotNull(two, "Tried to check chunks in null location two.");
        Preconditions.checkArgument(inSameWorld(one, two), "Tried to check chunks between worlds.");

        // Basic starting location check
        final var world = Preconditions.checkNotNull(one.getWorld(), "Tried to check chunks in null world.");
        if (!isChunkLoaded(world, one.getBlockX(), one.getBlockZ())) return false;

        final double totalXDistance = two.getX() - one.getX();
        final double totalZDistance = two.getZ() - one.getZ();

        final double xStep;
        final double zStep;

        final int steps;

        final boolean modifyX = Math.abs(totalXDistance) <= Math.abs(totalZDistance);

        if (modifyX) {
            steps = DoubleMath.roundToInt(Math.abs(totalZDistance), RoundingMode.CEILING);
            xStep = totalXDistance / steps;
            zStep = Math.signum(totalZDistance);
        } else {
            steps = DoubleMath.roundToInt(Math.abs(totalXDistance), RoundingMode.CEILING);
            xStep = Math.signum(totalXDistance);
            zStep = totalZDistance / steps;
        }

        double workingX = one.getX();
        double workingZ = one.getZ();
        double workingModifiedX;
        double workingModifiedZ;

        int chunkX;
        int chunkZ;

        // Cache the last and current chunk for faster processing.
        // The last chunk is important due to the modifier (inner loop) that checks any border behaviour and can have 2 chunks checked multiple times.
        int lastChunkX = toChunkCoordinate(workingX);
        int lastChunkZ = toChunkCoordinate(workingZ);
        int lastLastChunkX = lastChunkX;
        int lastLastChunkZ = lastChunkZ;

        for (int i = 0; i < steps; ++i) {
            workingX += xStep;
            workingZ += zStep;

            // Modifier to make sure that border behaviour of BlockIterator is covered.
            for (int modifier = -1; modifier <= 1; ++modifier) {
                workingModifiedX = workingX;
                workingModifiedZ = workingZ;

                if (modifyX) workingModifiedX += modifier;
                else workingModifiedZ += modifier;

                chunkX = toChunkCoordinate(workingModifiedX);
                chunkZ = toChunkCoordinate(workingModifiedZ);

                // If we have already checked the chunk, skip it, as we know it is loaded.
//                if (lastChunkX == chunkX && lastChunkZ == chunkZ ||
//                    lastLastChunkX == chunkX && lastLastChunkZ == chunkZ) continue;

                // Changed the if Condition as i was complex and not readble , Refactored it by using a decomposed conition
                if (isSameAsLastChunk(lastChunkX, lastChunkZ, chunkX, chunkZ)) continue;
                if (isSameAsSecondLastChunk(lastLastChunkX, lastLastChunkZ, chunkX, chunkZ)) continue;



                // A new chunk, check if it is loaded.
                if (!world.isChunkLoaded(chunkX, chunkZ)) return false;

                lastLastChunkX = lastChunkX;
                lastLastChunkZ = lastChunkZ;
                lastChunkX = chunkX;
                lastChunkZ = chunkZ;
            }
        }

        return true;
    }

    @Override
    public boolean areLocationsInRange(Location firstLocation, Location secondLocation, double distance)
    {
        return inSameWorld(firstLocation, secondLocation) &&
               firstLocation.distanceSquared(secondLocation) <= (distance * distance);
    }

    @Override
    public boolean isInventoryOpenable(Block block)
    {
        // The block is actually holding an inventory (and therefore is not e.g. dirt)
        if (!(block.getState() instanceof InventoryHolder)) return false;

        // Chiseled Bookshelf and decorated pot false positive fix.
        if (MaterialUtil.INSTANCE.getNonOpenableInventories().contains(block.getType())) return false;

        // Additional checks for cats and occluding blocks necessary?
        if (MaterialUtil.INSTANCE.getFreeSpaceContainers().contains(block.getType())) {
            final Block aboveBlock = block.getRelative(BlockFace.UP);

//            if (ServerVersion.MC112.activeIsEarlierOrEqual()) {
//                // 1.8.8 and 1.12 doesn't provide isPassable.
//                // Make sure that the block above is not obstructed by blocks
//                // Cannot check for cats on 1.8 and 1.12 as the server version doesn't provide the newer methods.
//                return !aboveBlock.getType().isOccluding();
//            } else {
//                // Any occluding block above will prevent the inventory from being opened.
//                return !aboveBlock.getType().isOccluding() &&
//                       // No cats or ocelots sitting on the chest.
//                       // Check the center of the block above with a radius of 0.5 blocks.
//                       aboveBlock.getWorld().getNearbyEntities(aboveBlock.getLocation().add(0.5, 0.5, 0.5), 0.5, 0.5, 0.5, entity -> switch (entity.getType()) {
//                           case CAT, OCELOT -> true;
//                           default -> false;
//                       }).isEmpty();
//            }


            // Delegate to polymorphic checker to handle version-specific logic (pre-1.12 vs. post-1.12),
            // removing imperative details from this method and improving abstraction.
            return inventoryChecker.canOpenInventory(block, aboveBlock);
        }
        return true;
    }

    @Override
    public List<LivingEntity> getLivingEntitiesAroundEntity(Entity entity, double x, double y, double z)
    {
        // Streaming here as the returned list of getNearbyEntities is unmodifiable, therefore streaming reduces code
        // complexity.
        return entity.getNearbyEntities(x, y, z).stream()
                     .filter(LivingEntity.class::isInstance)
                     .map(LivingEntity.class::cast)
                     .toList();
    }

    // New decomposed Condition Method
    private boolean isSameAsLastChunk(int lastChunkX, int lastChunkZ, int chunkX, int chunkZ) {
        return lastChunkX == chunkX && lastChunkZ == chunkZ;
    }
    // new decomposed condition Method
    private boolean isSameAsSecondLastChunk(int lastLastChunkX, int lastLastChunkZ,int chunkX, int chunkZ) {
        return lastLastChunkX == chunkX && lastLastChunkZ == chunkZ;
    }

}

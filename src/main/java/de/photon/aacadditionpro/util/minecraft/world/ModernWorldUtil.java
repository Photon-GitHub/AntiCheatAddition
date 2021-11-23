package de.photon.aacadditionpro.util.minecraft.world;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.exception.UnknownMinecraftException;
import de.photon.aacadditionpro.util.minecraft.entity.EntityUtil;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.InventoryHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class ModernWorldUtil implements WorldUtil
{
    @Override
    public boolean isNext(Block a, Block b, Set<BlockFace> faces)
    {
        Preconditions.checkNotNull(a, "Tried to check whether null block is next to another block.");
        Preconditions.checkNotNull(b, "Tried to check whether null block is next to another block.");
        Preconditions.checkNotNull(faces, "Tried to check whether block is next to another block at null faces.");
        return a.getWorld().getUID().equals(b.getWorld().getUID()) && faces.contains(a.getFace(b));
    }

    @Override
    public byte countBlocksAround(Block block, Set<BlockFace> faces, Set<Material> ignored)
    {
        Preconditions.checkNotNull(block, "Tried to count surrounding blocks of null block.");
        Preconditions.checkNotNull(faces, "Tried to count surrounding blocks of block at null faces.");
        Preconditions.checkNotNull(faces, "Tried to count surrounding blocks of block will null ignore (use empty set).");

        byte count = 0;
        for (BlockFace face : faces) {
            val b = block.getRelative(face);
            if (!b.isEmpty() && !ignored.contains(b.getType())) ++count;
        }
        return count;
    }

    @Override
    public List<Block> getBlocksAround(Block block, Set<BlockFace> faces, Set<Material> ignored)
    {
        Preconditions.checkNotNull(block, "Tried to get surrounding blocks of null block.");
        Preconditions.checkNotNull(faces, "Tried to get surrounding blocks of block at null faces.");
        Preconditions.checkNotNull(faces, "Tried to get surrounding blocks of block will null ignore (use empty set).");

        val result = new ArrayList<Block>();
        for (BlockFace face : faces) {
            val b = block.getRelative(face);
            if (!b.isEmpty() && !ignored.contains(b.getType())) result.add(b);
        }
        return List.copyOf(result);
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

    @Override
    public boolean areChunksLoadedBetweenLocations(Location one, Location two)
    {
        Preconditions.checkNotNull(one, "Tried to check chunks in null location one.");
        Preconditions.checkNotNull(two, "Tried to check chunks in null location two.");
        Preconditions.checkArgument(inSameWorld(one, two), "Tried to check chunks between worlds.");

        // Basic starting location check
        if (!isChunkLoaded(one.getWorld(), one.getBlockX(), one.getBlockZ())) return false;

        final boolean modifyX;

        val deltaX = two.getX() - one.getX();
        val deltaZ = two.getZ() - one.getZ();

        final double xStep;
        final double zStep;

        final int steps;

        if (Math.abs(deltaX) > Math.abs(deltaZ)) {
            modifyX = false;
            steps = (int) Math.ceil(Math.abs(deltaX));
            xStep = Math.signum(deltaX);
            zStep = deltaZ / steps;
        } else {
            modifyX = true;
            steps = (int) Math.ceil(Math.abs(deltaZ));
            xStep = deltaX / Math.abs(deltaZ);
            zStep = Math.signum(deltaZ);
        }

        double workingX = one.getX();
        double workingZ = one.getZ();
        double workingModifiedX;
        double workingModifiedZ;

        int chunkX;
        int chunkZ;

        // Cache the last and current chunk for faster processing.
        // The last chunk is important as of the modifier.
        final int[] currentChunkCoords = {((int) Math.floor(workingX)) >> 4, ((int) Math.floor(workingZ)) >> 4};
        final int[] lastChunkCoords = {currentChunkCoords[0], currentChunkCoords[1]};

        for (int i = 0; i < steps; ++i) {
            workingX += xStep;
            workingZ += zStep;

            // Modifier to make sure that border behaviour of BlockIterator is covered.
            for (int modifier = -1; modifier <= 1; ++modifier) {
                if (modifyX) {
                    workingModifiedX = workingX + modifier;
                    workingModifiedZ = workingZ;
                } else {
                    workingModifiedX = workingX;
                    workingModifiedZ = workingZ + modifier;
                }

                chunkX = ((int) Math.floor(workingModifiedX)) >> 4;
                chunkZ = ((int) Math.floor(workingModifiedZ)) >> 4;

                // Check if the chunk has already been checked
                if ((currentChunkCoords[0] != chunkX || currentChunkCoords[1] != chunkZ) &&
                    (lastChunkCoords[0] != chunkX || lastChunkCoords[1] != chunkZ))
                {
                    lastChunkCoords[0] = currentChunkCoords[0];
                    lastChunkCoords[1] = currentChunkCoords[1];
                    currentChunkCoords[0] = chunkX;
                    currentChunkCoords[1] = chunkZ;

                    if (!one.getWorld().isChunkLoaded(chunkX, chunkZ)) return false;
                }
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
        // The block actually is holding an inventory (and therefore is not e.g. dirt)
        if (block.getState() instanceof InventoryHolder) {
            // Additional checks for cats and occluding blocks necessary?
            if (MaterialUtil.FREE_SPACE_CONTAINERS.contains(block.getType())) {
                final Block aboveBlock = block.getRelative(BlockFace.UP);
                final Location checkForCatLocation = aboveBlock.getLocation().add(0.5, 0.5, 0.5);

                switch (ServerVersion.getActiveServerVersion()) {
                    case MC18:
                    case MC112:
                        // 1.8.8 and 1.12 doesn't provide isPassable.
                        // Make sure that the block above is not obstructed by blocks
                        // Cannot check for cats on 1.8 and 1.12 as the server version doesn't provide the newer methods.
                        return MaterialUtil.FREE_SPACE_CONTAINERS_ALLOWED_MATERIALS.contains(aboveBlock.getType());
                    case MC113:
                        // Make sure that the block above is not obstructed by blocks
                        return (aboveBlock.isPassable() || MaterialUtil.FREE_SPACE_CONTAINERS_ALLOWED_MATERIALS.contains(aboveBlock.getType()))
                               // Make sure that the block above is not obstructed by cats
                               && aboveBlock.getWorld().getNearbyEntities(checkForCatLocation, 0.5, 0.5, 0.5, EntityUtil.INSTANCE.ofType(EntityType.OCELOT)).isEmpty();
                    case MC114:
                    case MC115:
                    case MC116:
                    case MC117:
                        // Make sure that the block above is not obstructed by blocks
                        return (aboveBlock.isPassable() || MaterialUtil.FREE_SPACE_CONTAINERS_ALLOWED_MATERIALS.contains(aboveBlock.getType()))
                               // Make sure that the block above is not obstructed by cats
                               && aboveBlock.getWorld().getNearbyEntities(checkForCatLocation, 0.5, 0.5, 0.5, EntityUtil.INSTANCE.ofType(EntityType.CAT)).isEmpty();
                    default:
                        throw new UnknownMinecraftException();
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public List<LivingEntity> getLivingEntitiesAroundEntity(Entity entity, double x, double y, double z)
    {
        // Streaming here as the returned list of getNearbyEntities is unmodifiable, therefore streaming reduces code
        // complexity.
        return entity.getNearbyEntities(x, y, z).stream()
                     .filter(LivingEntity.class::isInstance)
                     .map(LivingEntity.class::cast)
                     .collect(Collectors.toUnmodifiableList());
    }
}

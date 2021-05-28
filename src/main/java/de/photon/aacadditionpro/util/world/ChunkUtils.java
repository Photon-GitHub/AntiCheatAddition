package de.photon.aacadditionpro.util.world;

import com.google.common.base.Preconditions;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChunkUtils
{

    /**
     * Checks if the chunk of a certain {@link Location} is loaded.
     *
     * @return True if the chunk is loaded, else false.
     */
    public static boolean isChunkLoaded(@NotNull final Location location)
    {
        Preconditions.checkNotNull(location, "Tried to check chunks in null location.");
        Preconditions.checkNotNull(location.getWorld(), "Tried to check chunks null world of a location.");
        return isChunkLoaded(location.getWorld(), location.getBlockX(), location.getBlockZ());
    }

    /**
     * Checks if the chunk of certain coordinates are loaded.
     *
     * @param world  the {@link World} to check for the chunk
     * @param blockX the x - coordinate of a {@link org.bukkit.block.Block} in that {@link World}
     * @param blockZ the z - coordinate of a {@link org.bukkit.block.Block} in that {@link World}
     *
     * @return True if the chunk is loaded, else false.
     */
    public static boolean isChunkLoaded(@NotNull final World world, final int blockX, final int blockZ)
    {
        Preconditions.checkNotNull(world, "Tried to check chunks in null world.");
        return world.isChunkLoaded(blockX >> 4, blockZ >> 4);
    }

    /**
     * Checks if the chunks between two locations are loaded without trying to load them.
     * This method should be used to see if a calculation is safe for async usage.
     */
    public static boolean areChunksLoadedBetweenLocations(@NotNull final Location one, @NotNull final Location two)
    {
        Preconditions.checkNotNull(one, "Tried to check chunks in null location one.");
        Preconditions.checkNotNull(two, "Tried to check chunks in null location two.");
        Preconditions.checkNotNull(one.getWorld(), "Tried to check chunks null world of a location.");
        Preconditions.checkArgument(LocationUtils.inSameWorld(one, two), "Tried to check chunks between worlds.");

        // Basic starting location check
        if (!isChunkLoaded(one.getWorld(), one.getBlockX(), one.getBlockZ())) return false;

        final boolean modifyX;

        final double deltaX = two.getX() - one.getX();
        final double deltaZ = two.getZ() - one.getZ();

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
}

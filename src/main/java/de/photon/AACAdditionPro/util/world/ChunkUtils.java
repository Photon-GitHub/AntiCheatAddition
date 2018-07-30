package de.photon.AACAdditionPro.util.world;

import org.bukkit.Location;
import org.bukkit.World;

public final class ChunkUtils
{
    /**
     * Checks if the chunk of a certain {@link Location} is loaded.
     *
     * @return True if the chunk is loaded, else false.
     */
    public static boolean isChunkLoaded(final Location location)
    {
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
    public static boolean isChunkLoaded(final World world, final int blockX, final int blockZ)
    {
        return world.isChunkLoaded(blockX >> 4, blockZ >> 4);
    }

    /**
     * Checks if the chunks between two locations are loaded without trying to load them.
     * This method should be used to see if a calculation is safe for async usage.
     */
    public static boolean areChunksLoadedBetweenLocations(Location one, Location two)
    {
        one.setY(0);
        two.setY(0);

        if (!one.getWorld().getUID().equals(two.getWorld().getUID()))
        {
            throw new IllegalArgumentException("Tried to check chunks between worlds.");
        }

        // Convert block coordinates to chunk coordinates
        final int oneChunkX = one.getBlockX() >> 4;
        final int oneChunkZ = one.getBlockZ() >> 4;

        final int twoChunkX = two.getBlockX() >> 4;
        final int twoChunkZ = two.getBlockZ() >> 4;

        // Get the chunk coordinate
        final double xDiff = Math.abs(oneChunkX - twoChunkX);
        final double zDiff = Math.abs(oneChunkZ - twoChunkZ);

        // Get the iteration directions.
        // 0.5 to ensure chunks on the boarder are still checked.
        final double xDir = oneChunkX > twoChunkX ? -0.5 : 0.5;
        final double zDir = oneChunkZ > twoChunkZ ? -0.5 : 0.5;

        double[] chunkCoords = new double[]{oneChunkX, oneChunkZ};

        int lastChunkX = Integer.MIN_VALUE;
        int lastChunkZ = Integer.MIN_VALUE;

        if (xDiff > zDiff)
        {
            double zIncrease = (zDiff / xDiff) * zDir;

            for (int i = 0; i <= xDiff; i++)
            {
                final int curChunkX = (int) Math.floor(chunkCoords[0]);
                final int curChunkZ = (int) Math.floor(chunkCoords[1]);

                if (curChunkX != lastChunkX || curChunkZ != lastChunkZ)
                {
                    if (!one.getWorld().isChunkLoaded(curChunkX, curChunkZ))
                    {
                        return false;
                    }

                    lastChunkX = curChunkX;
                    lastChunkZ = curChunkZ;
                }

                chunkCoords[0] += xDir;
                chunkCoords[1] += zIncrease;
            }
        }
        else
        {
            double xIncrease = (xDiff / zDiff) * xDir;

            for (int i = 0; i <= zDiff; i++)
            {
                final int curChunkX = (int) Math.floor(chunkCoords[0]);
                final int curChunkZ = (int) Math.floor(chunkCoords[1]);

                if (curChunkX != lastChunkX || curChunkZ != lastChunkZ)
                {
                    if (!one.getWorld().isChunkLoaded(curChunkX, curChunkZ))
                    {
                        return false;
                    }

                    lastChunkX = curChunkX;
                    lastChunkZ = curChunkZ;
                }

                chunkCoords[0] += xIncrease;
                chunkCoords[1] += zDir;
            }
        }
        return true;
    }
}

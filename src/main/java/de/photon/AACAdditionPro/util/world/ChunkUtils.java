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

    //TODO: Check unnecessary calls (i.e. wrong chunks)!!!
    public static boolean areChunksLoadedBetweenLocations(Location one, Location two)
    {
        one.setY(0);
        two.setY(0);

        if (!one.getWorld().getUID().equals(two.getWorld().getUID()))
        {
            throw new IllegalArgumentException("Tried to check chunks between worlds.");
        }

        int oneChunkX = one.getBlockX() >> 4;
        int oneChunkZ = one.getBlockZ() >> 4;

        int twoChunkX = two.getBlockX() >> 4;
        int twoChunkZ = two.getBlockZ() >> 4;

        double xDiff = Math.abs(oneChunkX - twoChunkX);
        double zDiff = Math.abs(oneChunkZ - twoChunkZ);

        // Get the iteration directions.
        final int xDir = oneChunkX > twoChunkX ? -1 : 1;
        final int zDir = oneChunkZ > twoChunkZ ? -1 : 1;

        double[] chunkCoords = new double[]{oneChunkX, oneChunkZ};
        if (xDiff > zDiff)
        {
            double zIncrease = (zDiff / xDiff) * zDir;
            System.out.println("zIncrease: " + zIncrease);

            for (int i = 0; i <= xDiff; i++)
            {
                System.out.println("Chunk: " + chunkCoords[0] + " | " + chunkCoords[1]);
                if (!one.getWorld().isChunkLoaded((int) Math.floor(chunkCoords[0]), (int) Math.floor(chunkCoords[1])))
                {
                    System.out.println("Not loaded");
                    return false;
                }

                System.out.println("Loaded");
                chunkCoords[0] += xDir;
                chunkCoords[1] += zIncrease;
            }
        }
        else
        {
            double xIncrease = (xDiff / zDiff) * xDir;
            System.out.println("xIncrease: " + xIncrease);

            for (int i = 0; i <= zDiff; i++)
            {
                System.out.println("Chunk: " + chunkCoords[0] + " | " + chunkCoords[1]);
                if (!one.getWorld().isChunkLoaded((int) Math.floor(chunkCoords[0]), (int) Math.floor(chunkCoords[1])))
                {
                    System.out.println("Not loaded");
                    System.out.println("----END----");
                    return false;
                }

                System.out.println("Loaded");
                chunkCoords[0] += xIncrease;
                chunkCoords[1] += zDir;
            }
        }

        System.out.println("----END----");

        return true;
    }
}

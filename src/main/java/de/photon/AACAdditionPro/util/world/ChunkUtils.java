package de.photon.AACAdditionPro.util.world;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

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

    public static boolean areChunksLoadedBetweenLocations(Location one, Location two)
    {
        one.setY(0);
        two.setY(0);

        if (!one.getWorld().getUID().equals(two.getWorld().getUID()))
        {
            throw new IllegalArgumentException("Tried to check chunks between worlds.");
        }

        List<Vector> chunkVectorList;

        if (Math.abs(two.getBlockZ() - one.getBlockZ()) < Math.abs(two.getBlockX() - one.getBlockX()))
        {
            chunkVectorList = one.getBlockX() > two.getBlockX() ?
                              getChunkCoordinatesLow(two.getBlockX(), two.getBlockZ(), one.getBlockX(), one.getBlockZ()) :
                              getChunkCoordinatesLow(one.getBlockX(), one.getBlockZ(), two.getBlockX(), two.getBlockZ());
        }
        else
        {
            chunkVectorList = one.getBlockZ() > two.getBlockZ() ?
                              getChunkCoordinatesHigh(two.getBlockX(), two.getBlockZ(), one.getBlockX(), one.getBlockZ()) :
                              getChunkCoordinatesHigh(one.getBlockX(), one.getBlockZ(), two.getBlockX(), two.getBlockZ());
        }

        // Actually check if the chunks are loaded.
        for (Vector vector : chunkVectorList)
        {
            System.out.println("Chunk: " + vector.getBlockX() + " " + vector.getBlockZ());
            if (!isChunkLoaded(one.getWorld(), vector.getBlockX(), vector.getBlockZ()))
            {
                return false;
            }
        }
        return true;
    }

    private static List<Vector> getChunkCoordinatesLow(int blockXOne, int blockZOne, int blockXTwo, int blockZTwo)
    {
        final List<Vector> vectors = new ArrayList<>();

        int dX = blockXTwo - blockXOne;
        int dZ = blockZTwo - blockZOne;

        int zI = 1;

        if (dZ < 0)
        {
            zI = -1;
            dZ = -dZ;
        }

        int distance = 2 * dZ - dX;
        int z = blockZOne;

        for (int x = blockXOne; x <= blockXTwo; x++)
        {
            vectors.add(new Vector(x, 0, z));

            if (distance > 0)
            {
                z += zI;
                distance -= 2 * dX;
            }
            distance += 2 * dZ;
        }
        return vectors;
    }

    private static List<Vector> getChunkCoordinatesHigh(int blockXOne, int blockZOne, int blockXTwo, int blockZTwo)
    {
        final List<Vector> vectors = new ArrayList<>();

        int dX = blockXTwo - blockXOne;
        int dZ = blockZTwo - blockZOne;

        int xI = 1;

        if (dX < 0)
        {
            xI = -1;
            dX = -dX;
        }

        int distance = 2 * dX - dZ;
        int x = blockXOne;

        for (int z = blockZOne; z <= blockZTwo; z++)
        {
            vectors.add(new Vector(x, 0, z));

            if (distance > 0)
            {
                x += xI;
                distance -= 2 * dZ;
            }
            distance += 2 * dX;
        }
        return vectors;
    }
}

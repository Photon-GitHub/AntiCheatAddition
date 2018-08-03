package de.photon.AACAdditionPro.util.mathematics;

import de.photon.AACAdditionPro.util.world.BlockUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public final class VectorUtils
{
    // The camera offset for 3rd person
    private static final double THIRD_PERSON_OFFSET = 5D;

    /**
     * Get to know where the {@link Vector} intersects with a {@link org.bukkit.block.Block}.
     * Non-Occluding {@link Block}s as defined in {@link BlockUtils#isReallyOccluding(Material)} are ignored.
     *
     * @param start     the starting {@link Location}
     * @param direction the {@link Vector} which should be checked
     *
     * @return The length when the {@link Vector} intersects or 0 if no intersection was found
     */
    public static double getDistanceToFirstIntersectionWithBlock(final Location start, final Vector direction)
    {
        final int length = (int) Math.floor(direction.length());
        if (length >= 1)
        {
            try
            {
                final BlockIterator blockIterator = new BlockIterator(start.getWorld(), start.toVector(), direction, 0, length);
                Block block;
                while (blockIterator.hasNext())
                {
                    block = blockIterator.next();
                    // Account for a Spigot bug: BARRIER and MOB_SPAWNER are not occluding blocks
                    if (BlockUtils.isReallyOccluding(block.getType()))
                    {
                        // Use the middle location of the Block instead of the simple location.
                        System.out.print("Intersection-Block: " + block.getLocation().clone().add(0.5, 0.5, 0.5).toVector());
                        return block.getLocation().clone().add(0.5, 0.5, 0.5).distance(start);
                    }
                }
            } catch (IllegalStateException exception)
            {
                // Just in case the start block could not be found for some reason or a chunk is loaded async.
                return 0;
            }
        }
        return 0;
    }

    /**
     * @return an array of {@link Vector}s which represent the 3 different camera modes in minecraft, 1st person and the two
     * 3rd person views with the following indices: <br>
     * [0] = normal (eyeposition vector) <br>
     * [1] = front <br>
     * [2] = behind <br>
     */
    public static Vector[] getCameraVectors(final Player player)
    {
        /*
            All the vectors
            [0] = normal (eyeposition vector)
            [1] = front
            [2] = behind
        */
        final Vector[] vectors = new Vector[3];

        // Front vector : The 3rd person perspective in front of the player
        // Use THIRD_PERSON_OFFSET to get the maximum positions
        // No cloning or normalizing as a new unit-vector instance is returned.
        vectors[1] = player.getLocation().getDirection().multiply(THIRD_PERSON_OFFSET);

        // Behind vector : The 3rd person perspective behind the player
        vectors[2] = vectors[1].clone().multiply(-1);

        final Location eyeLocation = player.getEyeLocation();

        // Normal
        vectors[0] = eyeLocation.toVector();

        // Do the Cameras intersect with Blocks
        // Get the length of the first intersection or 0 if there is none

        // [0] = frontIntersection
        // [1] = behindIntersection
        final double[] intersections = new double[]{
                VectorUtils.getDistanceToFirstIntersectionWithBlock(eyeLocation, vectors[1]),
                VectorUtils.getDistanceToFirstIntersectionWithBlock(eyeLocation, vectors[2])
        };

        for (int i = 0; i < intersections.length; i++)
        {
            // There is an intersection
            if (intersections[i] != 0)
            {
                // Now we need to make sure the vectors are not inside of blocks as the method above returns.
                // The 0.05 factor makes sure that we are outside of the block and not on the edge.
                intersections[i] -= 0.05 +
                                    // Calculate the distance to the middle of the block
                                    (0.5 / Math.sin(vectors[i + 1].angle(vectors[i + 1].clone().setY(0))));

                // Add the correct position.
                vectors[i + 1].normalize().multiply(intersections[i]);
            }

            // Add the eye location for a correct starting point.
            vectors[i + 1].add(vectors[0]);
        }
        return vectors;
    }
}
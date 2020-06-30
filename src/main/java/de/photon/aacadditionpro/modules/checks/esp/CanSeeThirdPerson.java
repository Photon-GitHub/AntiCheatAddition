package de.photon.aacadditionpro.modules.checks.esp;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.function.Function;

class CanSeeThirdPerson implements Function<Player, Vector[]>
{
    private static final double THIRD_PERSON_OFFSET = 5D;

    @Override
    public Vector[] apply(Player player)
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
                EspUtil.getDistanceToFirstIntersectionWithBlock(eyeLocation, vectors[1]),
                EspUtil.getDistanceToFirstIntersectionWithBlock(eyeLocation, vectors[2])
        };

        for (int i = 0; i < intersections.length; i++) {
            // There is an intersection
            if (intersections[i] != 0) {
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

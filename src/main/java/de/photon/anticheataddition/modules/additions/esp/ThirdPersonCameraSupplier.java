package de.photon.anticheataddition.modules.additions.esp;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

class ThirdPersonCameraSupplier implements CameraVectorSupplier
{
    private static final double THIRD_PERSON_OFFSET = 5D;

    @Override
    public Location[] getCameraLocations(Player player)
    {
        /*
            All the vectors
            [0] = normal (eye position vector)
            [1] = front
            [2] = behind
        */
        final Location[] locations = new Location[3];
        final World world = player.getWorld();

        // Front vector : The 3rd person perspective in front of the player
        // Use THIRD_PERSON_OFFSET to get the maximum positions
        // No cloning or normalizing as a new unit-vector instance is returned.
        locations[1] = player.getLocation().getDirection().multiply(THIRD_PERSON_OFFSET).toLocation(world);

        // Behind vector : The 3rd person perspective behind the player
        locations[2] = locations[1].clone().multiply(-1);

        final Location eyeLocation = player.getEyeLocation();

        // Normal
        locations[0] = eyeLocation;

        // Do the Cameras intersect with Blocks
        // Get the length of the first intersection or 0 if there is none

        // [0] = frontIntersection
        // [1] = behindIntersection
        final double[] intersections = new double[]{
                CameraVectorSupplier.getDistanceToFirstIntersectionWithBlock(eyeLocation, locations[1].toVector()),
                CameraVectorSupplier.getDistanceToFirstIntersectionWithBlock(eyeLocation, locations[2].toVector())
        };

        for (int i = 0; i < intersections.length; ++i) {
            // There is an intersection
            if (intersections[i] != 0) {
                // Now we need to make sure the vectors are not inside of blocks as the method above returns.
                // The 0.05 factor makes sure that we are outside the block and not on the edge.
                intersections[i] -= 0.05 +
                                    // Calculate the distance to the middle of the block
                                    (0.5 / Math.sin(locations[i + 1].toVector().angle(locations[i + 1].toVector().clone().setY(0))));

                // Add the correct position.
                locations[i + 1].toVector().normalize().multiply(intersections[i]);
            }

            // Add the eye location for a correct starting point.
            locations[i + 1].add(locations[0]);
        }
        return locations;
    }

    @Override
    public boolean ignoreFOV()
    {
        return true;
    }
}

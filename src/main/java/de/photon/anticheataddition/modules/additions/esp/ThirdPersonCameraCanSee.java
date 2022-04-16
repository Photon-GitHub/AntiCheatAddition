package de.photon.anticheataddition.modules.additions.esp;

import de.photon.anticheataddition.util.mathematics.Hitbox;
import de.photon.anticheataddition.util.mathematics.ResetVector;
import de.photon.anticheataddition.util.minecraft.world.InternalPotion;
import de.photon.anticheataddition.util.minecraft.world.WorldUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

final class ThirdPersonCameraCanSee implements CanSee
{
    private static final double THIRD_PERSON_OFFSET = 5D;

    private static Location[] getCameraLocations(Player player)
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
                CanSee.getDistanceToFirstIntersectionWithBlock(eyeLocation, locations[1].toVector()),
                CanSee.getDistanceToFirstIntersectionWithBlock(eyeLocation, locations[2].toVector())
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
    public boolean canSee(Player observer, Player watched)
    {
        // Glowing.
        if (InternalPotion.GLOWING.hasPotionEffect(watched)) return true;

        // ----------------------------------- Calculation ---------------------------------- //
        final Location[] watchedHitboxLocations = Hitbox.espHitboxLocationOf(watched).getEspLocations();

        for (Location cameraLocation : getCameraLocations(observer)) {
            final ResetVector between = new ResetVector(cameraLocation.toVector().multiply(-1));
            for (Location hitLoc : watchedHitboxLocations) {
                // Effectively hitLoc - cameraLocation because of the multiply(-1) above.
                between.resetToBase().add(hitLoc.toVector());

                // Ignore FOV checking as 3rd person has a look back option.

                // Make sure the chunks are loaded.
                // If the chunks are not loaded assume the players can see each other.
                if (!WorldUtil.INSTANCE.areChunksLoadedBetweenLocations(cameraLocation, hitLoc)) return true;

                // No intersection found
                if (CanSee.canSeeHeuristic(cameraLocation, between, hitLoc)) return true;
            }
        }
        return false;
    }
}

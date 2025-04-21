package de.photon.anticheataddition.modules.additions.esp;
import de.photon.anticheataddition.util.mathematics.Hitbox;
import de.photon.anticheataddition.util.mathematics.ResetVector;
import de.photon.anticheataddition.util.minecraft.world.OcclusionUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/**
 * Checks if a player can see another player in third-person camera view.
 */
final class ThirdPersonCameraCanSee implements CanSee {

    /** Maximum distance for third-person camera in Minecraft */
    private static final int MAX_THIRD_PERSON_DISTANCE = 5;

    /** Offset used to prevent the camera from entering blocks */
    private static final double CAMERA_COLLISION_OFFSET = 0.5D;

    /**
     * Calculates the actual camera distance considering block intersections.
     */
    private static double getCameraDistance(final Location eyeLocation, final Vector direction) {
        final double intersection = OcclusionUtil.getDistanceToFirstIntersectionWithBlock(
                eyeLocation, direction, MAX_THIRD_PERSON_DISTANCE);

        return intersection == 0 ? MAX_THIRD_PERSON_DISTANCE : intersection - CAMERA_COLLISION_OFFSET;
    }

    /**
     * Returns all possible camera positions for the player.
     * Eye level, forward-facing third-person, and backward-facing third-person.
     */
    private static Location[] getCameraLocations(final Player player) {
        final Location eyeLocation = player.getEyeLocation();
        final Vector direction = player.getLocation().getDirection();

        // Calculate vectors for forward and backward cameras
        final Vector forwardDir = direction.clone();
        final Vector backwardDir = direction.clone().multiply(-1);

        // Calculate distances the cameras can travel before hitting blocks
        final double forwardDistance = getCameraDistance(eyeLocation, forwardDir);
        final double backwardDistance = getCameraDistance(eyeLocation, backwardDir);

        // Create camera locations
        final Location[] cameraLocations = new Location[3];
        cameraLocations[0] = eyeLocation;
        cameraLocations[1] = eyeLocation.clone().add(forwardDir.normalize().multiply(forwardDistance));
        cameraLocations[2] = eyeLocation.clone().add(backwardDir.normalize().multiply(backwardDistance));

        return cameraLocations;
    }

    /**
     * Determines whether the observing player can see the watched player
     * from any third-person camera angle.
     */
    @Override
    public boolean canSeeTracing(Player observer, Player watched) {
        final Location[] hitboxLocations = Hitbox.espHitboxLocationOf(watched).getEspLocations();
        final Location[] cameraLocations = getCameraLocations(observer);

        // Check from each camera position
        for (Location cameraLocation : cameraLocations) {
            // Use negative camera position as base vector
            final ResetVector directionVector = new ResetVector(cameraLocation.toVector().multiply(-1));

            // Check visibility for each point in the target player's hitbox
            for (Location hitboxLocation : hitboxLocations) {
                // Calculate direction vector: hitboxLocation - cameraLocation
                directionVector.resetToBase().add(hitboxLocation.toVector());

                // Is the line of sight unobstructed?
                if (!OcclusionUtil.isRayOccluded(cameraLocation, directionVector, hitboxLocation)) {
                    return true; // At least one point is visible
                }
            }
        }

        return false; // No points are visible
    }
}
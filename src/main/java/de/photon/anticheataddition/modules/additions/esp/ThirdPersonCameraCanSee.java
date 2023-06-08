package de.photon.anticheataddition.modules.additions.esp;

import de.photon.anticheataddition.util.mathematics.Hitbox;
import de.photon.anticheataddition.util.mathematics.ResetVector;
import de.photon.anticheataddition.util.minecraft.world.OcclusionUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

final class ThirdPersonCameraCanSee implements CanSee
{
    private static final int THIRD_PERSON_OFFSET = 5;
    private static final double CAMERA_COLLISION_OFFSET = 0.5D;

    private static double getCameraDistance(final Location eyeLocation, final Vector direction)
    {
        final double intersection = OcclusionUtil.getDistanceToFirstIntersectionWithBlock(eyeLocation, direction, THIRD_PERSON_OFFSET);
        return intersection == 0 ? THIRD_PERSON_OFFSET : intersection - CAMERA_COLLISION_OFFSET;
    }

    private static Location[] getCameraLocations(final Player player)
    {
        final Location eyeLocation = player.getEyeLocation();

        final Vector lookingForward = player.getLocation().getDirection();
        final Vector lookingBehind = lookingForward.clone().multiply(-1);

        // Do the Cameras intersect with Blocks?
        // Get the length of the first intersection or 0 if there is none
        double forwardDistance = getCameraDistance(eyeLocation, lookingForward);
        double behindDistance = getCameraDistance(eyeLocation, lookingBehind);

        // All locations and camera rays go out from the eye location.
        final Location[] result = new Location[]{eyeLocation, eyeLocation.clone(), eyeLocation.clone()};

        // result[0] is the eye location and should not be changed.
        // Subtract half a block to make sure that our intersection is before and not in a block.
        result[1].add(lookingForward.normalize().multiply(forwardDistance));
        result[2].add(lookingBehind.normalize().multiply(behindDistance));

        return result;
    }

    @Override
    public boolean canSeeTracing(Player observer, Player watched)
    {
        // ----------------------------------- Calculation ---------------------------------- //
        final Location[] watchedHitboxLocations = Hitbox.espHitboxLocationOf(watched).getEspLocations();

        for (Location cameraLocation : getCameraLocations(observer)) {
            final ResetVector between = new ResetVector(cameraLocation.toVector().multiply(-1));
            for (Location hitLoc : watchedHitboxLocations) {
                // Effectively hitLoc - cameraLocation because of the multiply(-1) above.
                between.resetToBase().add(hitLoc.toVector());

                // Ignore FOV checking as 3rd person has a look back option.

                // No intersection found
                if (!OcclusionUtil.isRayOccluded(cameraLocation, between, hitLoc)) return true;
            }
        }
        return false;
    }
}

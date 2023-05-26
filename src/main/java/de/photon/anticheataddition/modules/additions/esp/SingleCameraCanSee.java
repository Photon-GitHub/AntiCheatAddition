package de.photon.anticheataddition.modules.additions.esp;

import de.photon.anticheataddition.util.mathematics.Hitbox;
import de.photon.anticheataddition.util.mathematics.ResetVector;
import de.photon.anticheataddition.util.minecraft.world.OcclusionUtil;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

final class SingleCameraCanSee implements CanSee
{
    // The real MAX_FOV is 110 (quake pro), which results in 137° according to https://minecraft.fandom.com/wiki/Options
    // + Compensation -> 165°
    // Now, as we use the view direction vector, only half of that is actually achievable as the vector is the "middle".
    private static final double MAX_FOV = Math.toRadians(165D / 2);

    @Override
    public boolean canSeeTracing(Player observer, Player watched)
    {
        // ----------------------------------- Calculation ---------------------------------- //
        final Vector viewDirection = observer.getLocation().getDirection();
        final Location cameraLocation = observer.getEyeLocation();

        final ResetVector between = new ResetVector(cameraLocation.toVector().multiply(-1));
        for (Location hitLoc : Hitbox.espHitboxLocationOf(watched).getEspLocations()) {
            // Effectively hitLoc - cameraLocation because of the multiply(-1) above.
            between.resetToBase().add(hitLoc.toVector());

            // Ignore directions that cannot be seen by the player due to FOV.
            if (viewDirection.angle(between) > MAX_FOV) continue;

            // No intersection found
            if (!OcclusionUtil.isRayOccluded(cameraLocation, between, hitLoc)) return true;
        }
        return false;
    }
}

package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.messaging.Log;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

final class ScaffoldAngle extends Module
{
    private static final double MAX_ANGLE = Math.toRadians(90);

    ScaffoldAngle(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Angle");
    }

    public int getVl(User user, BlockPlaceEvent event)
    {
        if (!this.isEnabled()) return 0;

        final var placedFace = event.getBlock().getFace(event.getBlockAgainst());
        final var placedVector = new Vector(placedFace.getModX(), placedFace.getModY(), placedFace.getModZ());

        // If greater than 90 in radians.
        if (user.getData().counter.scaffoldAngleFails.conditionallyIncDec(user.getPlayer().getLocation().getDirection().angle(placedVector) > MAX_ANGLE)) {
            Log.fine(() -> "Scaffold-Debug | Player: " + user.getPlayer().getName() + " placed a block with a suspicious angle.");
            return 15;
        }
        return 0;
    }
}

package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.log.Log;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.util.Vector;

final class ScaffoldAngle extends Module
{
    public static final ScaffoldAngle INSTANCE = new ScaffoldAngle();
    private static final double MAX_ANGLE = 90;

    private ScaffoldAngle()
    {
        super("Scaffold.parts.Angle");
    }

    public int getVl(User user, BlockPlaceEvent event)
    {
        if (!this.isEnabled()) return 0;

        final var face = event.getBlock().getFace(event.getBlockAgainst());
        if (face == null) return 0;

        final var placedVector = new Vector(face.getModX(), face.getModY(), face.getModZ());
        final var angle = Math.toDegrees(user.getPlayer().getEyeLocation().getDirection().angle(placedVector));

        Log.finer(() -> "Scaffold-Debug | Player: %s placed block with an angle of %.3f degrees.".formatted(user.getPlayer().getName(), angle));

        // Flag if the angle is too great as the player would not be able to target the block.
        if (user.getData().counter.scaffoldAngleFails.conditionallyIncDec(angle > MAX_ANGLE)) {
            Log.fine(() -> "Scaffold-Debug | Player: %s placed a block with a suspicious angle of %.3f degrees.".formatted(user.getPlayer().getName(), angle));
            return 15;
        }
        return 0;
    }
}

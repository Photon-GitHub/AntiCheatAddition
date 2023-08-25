package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.messaging.Log;

/**
 * This Scaffold part identifies suspicious rotation patterns sudden, large angle changes, as well as some very random rotations.
 */
public class ScaffoldRotation extends Module
{
    public static final ScaffoldRotation INSTANCE = new ScaffoldRotation();

    // Two full circles.
    private static final double ANGLE_CHANGE_SUM_THRESHOLD = 2 * Math.PI;
    private static final double ANGLE_VARIANCE_THRESHOLD = 0.3D;

    private ScaffoldRotation()
    {
        super("Scaffold.parts.Rotation");
    }

    public int getVl(User user)
    {
        if (!this.isEnabled()) return 0;
        int vl = 0;

        final var scaffoldAngleInfo = user.getLookPacketData().getAngleInformation();

        // Detect sudden changes in the last two ticks.
        if (user.getTimeMap().at(TimeKey.SCAFFOLD_SIGNIFICANT_ROTATION_CHANGE).recentlyUpdated(125)) {
            Log.fine(() -> "Scaffold-Debug | Player: " + user.getPlayer().getName() + " placed a block after a large, sudden rotation change.");
            vl += 15;
        }

        // Detects an excessive amount of large rotation changes in general.
        if (scaffoldAngleInfo.changeSum() > ANGLE_CHANGE_SUM_THRESHOLD) {
            Log.fine(() -> "Scaffold-Debug | Player: %s sent high rotation changes (%.3f).".formatted(user.getPlayer().getName(), scaffoldAngleInfo.changeSum()));
            vl += 10;
        }

        // A high variance can mean that the player is sending random rotations or is perfectly aiming at blocks.
        if (scaffoldAngleInfo.variance() > ANGLE_VARIANCE_THRESHOLD) {
            Log.fine(() -> "Scaffold-Debug | Player: %s sent rotations with very high variance (%.3f).".formatted(user.getPlayer().getName(), scaffoldAngleInfo.variance()));
            vl += 5;
        }

        return user.getData().counter.scaffoldRotationFails.conditionallyIncDec(vl > 0) ? vl : 0;
    }
}

package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.log.Log;

/**
 * This Scaffold part identifies suspicious rotation patterns sudden, large angle changes, as well as some very random rotations.
 */
public final class ScaffoldRotation extends Module
{
    public static final ScaffoldRotation INSTANCE = new ScaffoldRotation();

    // Thresholds in degrees.
    public static final double SIGNIFICANT_YAW_CHANGE = 35;
    private static final double ANGLE_CHANGE_SUM_THRESHOLD = 360;
    private static final double ANGLE_VARIANCE_THRESHOLD = 70;
    private static final double ANGLE_SWITCH_THRESHOLD = 20;

    private ScaffoldRotation()
    {
        super("Scaffold.parts.Rotation");
    }

    public int getVl(User user)
    {
        if (!this.isEnabled()) return 0;
        int vl = 0;

        final var scaffoldAngleInfoOptional = user.getLookPacketData().calculateRecentAngleStatistics();
        if (scaffoldAngleInfoOptional.isEmpty()) return 0;
        final var scaffoldAngleInfo = scaffoldAngleInfoOptional.get();

        // Detect sudden changes in the last two ticks.
        if (user.getData().counter.scaffoldRotationSignificantChangeFails.conditionallyIncDec(
                user.getTimeMap().at(TimeKey.SCAFFOLD_SIGNIFICANT_YAW_CHANGE).recentlyUpdated(125))) {
            Log.fine(() -> "Scaffold-Debug | Player: " + user.getPlayer().getName() + " placed a block after a large, sudden rotation change.");
            vl += 15;
        }

        // Detects an excessive amount of large rotation changes in general.
        if (user.getData().counter.scaffoldRotationAngleSumFails.conditionallyIncDec(scaffoldAngleInfo.deltaAngleChangeSum() > ANGLE_CHANGE_SUM_THRESHOLD)) {
            Log.fine(() -> "Scaffold-Debug | Player: %s sent high rotation changes (%.3f).".formatted(user.getPlayer().getName(), scaffoldAngleInfo.deltaAngleChangeSum()));
            vl += 10;
        }

        // A high variance can mean that the player is sending random rotations or is perfectly aiming at blocks.
        if (user.getData().counter.scaffoldRotationAngleVarianceFails.conditionallyIncDec(scaffoldAngleInfo.deltaAngleVariance() > ANGLE_VARIANCE_THRESHOLD)) {
            Log.fine(() -> "Scaffold-Debug | Player: %s sent rotations with very high variance (%.3f).".formatted(user.getPlayer().getName(), scaffoldAngleInfo.deltaAngleVariance()));
            vl += 5;
        }

        // Detects switches from left-side to right-side building.
        if (user.getData().counter.scaffoldRotationAngleSwitchFails.conditionallyIncDec(
                scaffoldAngleInfo.deltaAngleMagnitudes().stream().mapToDouble(Double::doubleValue)
                                 // We only care about larger rotations.
                                 .filter(angle -> angle > ANGLE_SWITCH_THRESHOLD).count() > 4)) {
            Log.fine(() -> "Scaffold-Debug | Player: %s sent rotation switches.".formatted(user.getPlayer().getName()));
            vl += 5;
        }

        return vl;
    }
}

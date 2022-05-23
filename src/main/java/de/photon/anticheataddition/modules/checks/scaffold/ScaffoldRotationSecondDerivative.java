package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.messaging.Log;

/**
 * This patterns detects very random rotations that some randomized scaffold modules might use.
 */
final class ScaffoldRotationSecondDerivative extends Module
{
    private static final double ANGLE_OFFSET_SUM_THRESHOLD = 5.2D;

    ScaffoldRotationSecondDerivative(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Rotation.SecondDerivative");
    }

    public int getVl(User user, double angleInformation)
    {
        if (!this.isEnabled()) return 0;

        if (angleInformation > ANGLE_OFFSET_SUM_THRESHOLD) {
            Log.fine(() -> "Scaffold-Debug | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 3");
            return 5;
        }
        return 0;
    }
}

package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.subdata.LookPacketData;
import de.photon.anticheataddition.util.messaging.Log;

/**
 * This patterns detects very random rotations that some randomized scaffold modules might use.
 */
final class ScaffoldRotationSecondDerivative extends Module
{
    public static final ScaffoldRotationSecondDerivative INSTANCE = new ScaffoldRotationSecondDerivative();
    private static final double ANGLE_OFFSET_SUM_THRESHOLD = 5.2D;

    private ScaffoldRotationSecondDerivative()
    {
        super("Scaffold.parts.Rotation.SecondDerivative");
    }

    public int getVl(User user, LookPacketData.ScaffoldAngleInfo scaffoldAngleInfo)
    {
        if (!this.isEnabled()) return 0;

        if (scaffoldAngleInfo.offsetSum() > ANGLE_OFFSET_SUM_THRESHOLD) {
            Log.fine(() -> "Scaffold-Debug | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 3");
            return 5;
        }
        return 0;
    }
}

package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.subdata.LookPacketData;
import de.photon.anticheataddition.util.messaging.Log;

/**
 * This pattern detects huge angle changes while scaffolding that
 * do not reflect legit behaviour.
 */
final class ScaffoldRotationDerivative extends Module
{
    public static final ScaffoldRotationDerivative INSTANCE = new ScaffoldRotationDerivative();

    private static final double ANGLE_CHANGE_SUM_THRESHOLD = 7D;

    private ScaffoldRotationDerivative()
    {
        super("Scaffold.parts.Rotation.Derivative");
    }

    public int getVl(User user, LookPacketData.ScaffoldAngleInfo scaffoldAngleInfo)
    {
        if (!this.isEnabled()) return 0;

        if (scaffoldAngleInfo.changeSum() > ANGLE_CHANGE_SUM_THRESHOLD) {
            Log.fine(() -> "Scaffold-Debug | Player: " + user.getPlayer().getName() + " sent suspicious rotation changes.");
            return 10;
        }
        return 0;
    }
}

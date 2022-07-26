package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.messaging.Log;

/**
 * This detects safe-walk behaviour (stopping when not sneaking)
 */
final class ScaffoldSafewalkTiming extends Module
{
    ScaffoldSafewalkTiming(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Safewalk.Timing");
    }

    public int getVl(User user)
    {
        if (!this.isEnabled()) return 0;

        if (user.getData().counter.scaffoldSafewalkTimingFails.conditionallyIncDec(
                // Moved recently
                user.hasMovedRecently(TimeKey.XZ_MOVEMENT, 355) &&
                // Suddenly stopped
                !user.hasMovedRecently(TimeKey.XZ_MOVEMENT, 175) &&
                // Has not sneaked recently
                !(user.hasSneakedRecently(175) && user.getData().number.lastSneakDuration > 148)))
        {
            Log.fine(() -> "Scaffold-Debug | Player: " + user.getPlayer().getName() + " has behaviour associated with safe-walk. (Timing)");
            return 20;
        }
        return 0;
    }
}

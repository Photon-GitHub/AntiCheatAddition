package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.messaging.Log;


/**
 * This pattern detects significant rotation "jumps" in the last two ticks.
 */
final class ScaffoldRotationFastChange extends Module
{
    ScaffoldRotationFastChange(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Rotation.FastChange");
    }

    public int getVl(User user)
    {
        if (!this.isEnabled()) return 0;

        if (user.getTimeMap().at(TimeKey.SCAFFOLD_SIGNIFICANT_ROTATION_CHANGE).recentlyUpdated(125)) {
            Log.fine(() -> "Scaffold-Debug | Player: " + user.getPlayer().getName() + " sent fast rotation changes.");
            return 15;
        }
        return 0;
    }
}

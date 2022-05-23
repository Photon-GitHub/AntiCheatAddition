package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataKey;
import de.photon.anticheataddition.util.messaging.Log;

/**
 * This pattern detects bursts of sprinting while scaffolding. No legit is able to properly utilize sprinting so far
 * because of the direction limitations.
 */
final class ScaffoldSprinting extends Module
{
    ScaffoldSprinting(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Sprinting");
    }

    public int getVl(User user)
    {
        if (!this.isEnabled()) return 0;

        if (user.getDataMap().getCounter(DataKey.Count.SCAFFOLD_SPRINTING_FAILS).conditionallyIncDec(user.hasSprintedRecently(400))) {
            Log.fine(() -> "Scaffold-Debug | Player: " + user.getPlayer().getName() + " sprinted suspiciously.");
            return 45;
        }
        return 0;
    }
}

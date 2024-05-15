package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.log.Log;

/**
 * This pattern detects bursts of sprinting while scaffolding. No legit is able to properly utilize sprinting so far
 * because of the direction limitations.
 */
final class ScaffoldSprinting extends Module
{
    public static final ScaffoldSprinting INSTANCE = new ScaffoldSprinting();

    private ScaffoldSprinting()
    {
        super("Scaffold.parts.Sprinting");
    }

    public int getVl(User user)
    {
        if (!this.isEnabled()) return 0;

        if (user.getData().counter.scaffoldSprintingFails.conditionallyIncDec(user.hasSprintedRecently(400))) {
            Log.fine(() -> "Scaffold-Debug | Player: " + user.getPlayer().getName() + " sprinted suspiciously.");
            return 45;
        }
        return 0;
    }
}

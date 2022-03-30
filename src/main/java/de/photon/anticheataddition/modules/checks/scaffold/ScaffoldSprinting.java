package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataKey;
import de.photon.anticheataddition.util.messaging.DebugSender;
import lombok.Getter;

import java.util.function.ToIntFunction;

/**
 * This pattern detects bursts of sprinting while scaffolding. No legit is able to properly utilize sprinting so far
 * because of the direction limitations.
 */
final class ScaffoldSprinting extends Module
{
    @Getter
    private ToIntFunction<User> applyingConsumer = user -> 0;

    ScaffoldSprinting(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Sprinting");
    }

    @Override
    public void enable()
    {
        applyingConsumer = user -> {
            if (user.getDataMap().getCounter(DataKey.Count.SCAFFOLD_SPRINTING_FAILS).conditionallyIncDec(user.hasSprintedRecently(400))) {
                DebugSender.getInstance().sendDebug("Scaffold-Debug | Player: " + user.getPlayer().getName() + " sprinted suspiciously.");
                return 45;
            }
            return 0;
        };
    }

    @Override
    public void disable()
    {
        applyingConsumer = user -> 0;
    }
}

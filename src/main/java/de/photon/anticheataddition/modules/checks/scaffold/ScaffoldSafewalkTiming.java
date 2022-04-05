package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataKey;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.messaging.DebugSender;
import lombok.Getter;

import java.util.function.ToIntFunction;

/**
 * This detects safe-walk behaviour (stopping when not sneaking)
 */
final class ScaffoldSafewalkTiming extends Module
{
    @Getter
    private ToIntFunction<User> applyingConsumer = user -> 0;

    ScaffoldSafewalkTiming(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Safewalk.Timing");
    }

    @Override
    public void enable()
    {
        applyingConsumer = user -> {
            if (user.getDataMap().getCounter(DataKey.Count.SCAFFOLD_SAFEWALK_TIMING_FAILS).conditionallyIncDec(
                    // Moved recently
                    user.hasMovedRecently(TimeKey.XZ_MOVEMENT, 355) &&
                    // Suddenly stopped
                    !user.hasMovedRecently(TimeKey.XZ_MOVEMENT, 175) &&
                    // Has not sneaked recently
                    !(user.hasSneakedRecently(175) && user.getDataMap().getLong(DataKey.Long.LAST_SNEAK_DURATION) > 148)))
            {
                DebugSender.INSTANCE.sendDebug("Scaffold-Debug | Player: " + user.getPlayer().getName() + " has behaviour associated with safe-walk. (Timing)");
                return 20;
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

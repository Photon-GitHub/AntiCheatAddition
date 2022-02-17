package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.oldmessaging.DebugSender;
import lombok.Getter;

import java.util.function.ToIntFunction;

/**
 * This detects safe-walk behaviour (stopping when not sneaking)
 */
class ScaffoldSafewalkTiming extends Module
{
    @Getter
    private ToIntFunction<User> applyingConsumer = user -> 0;

    public ScaffoldSafewalkTiming(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Safewalk.Timing");
    }

    @Override
    public void enable()
    {
        applyingConsumer = user -> {
            if (user.getDataMap().getCounter(DataKey.CounterKey.SCAFFOLD_SAFEWALK_TIMING_FAILS).conditionallyIncDec(
                    // Moved recently
                    user.hasMovedRecently(TimestampKey.LAST_XZ_MOVEMENT, 355) &&
                    // Suddenly stopped
                    !user.hasMovedRecently(TimestampKey.LAST_XZ_MOVEMENT, 175) &&
                    // Has not sneaked recently
                    !(user.hasSneakedRecently(175) && user.getDataMap().getLong(DataKey.LongKey.LAST_SNEAK_DURATION) > 148)))
            {
                DebugSender.getInstance().sendDebug("Scaffold-Debug | Player: " + user.getPlayer().getName() + " has behaviour associated with safe-walk. (Timing)");
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

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }
}

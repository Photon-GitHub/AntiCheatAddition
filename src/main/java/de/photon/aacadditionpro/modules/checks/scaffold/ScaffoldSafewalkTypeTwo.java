package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import lombok.Getter;

import java.util.function.ToIntFunction;

/**
 * This detects safe-walk behaviour (stopping when not sneaking)
 */
class ScaffoldSafewalkTypeTwo extends Module
{
    @Getter
    private ToIntFunction<User> applyingConsumer = user -> 0;

    public ScaffoldSafewalkTypeTwo(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Safewalk.type2");
    }

    @Override
    public void enable()
    {
        applyingConsumer = user -> {
            // Moved recently
            if (user.hasMovedRecently(TimestampKey.LAST_XZ_MOVEMENT, 355) &&
                // Suddenly stopped
                !user.hasMovedRecently(TimestampKey.LAST_XZ_MOVEMENT, 175) &&
                // Has not sneaked recently
                !(user.hasSneakedRecently(175) && user.getDataMap().getLong(DataKey.LongKey.LAST_SNEAK_DURATION) > 148))
            {
                if (user.getDataMap().getCounter(DataKey.CounterKey.SCAFFOLD_SAFEWALK_TYPE2_FAILS).incrementCompareThreshold()) {
                    DebugSender.getInstance().sendDebug("Scaffold-Debug | Player: " + user.getPlayer().getName() + " has behaviour associated with safe-walk. (Type 2)");
                    return 20;
                }
            } else user.getDataMap().getCounter(DataKey.CounterKey.SCAFFOLD_SAFEWALK_TYPE2_FAILS).decrementAboveZero();
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

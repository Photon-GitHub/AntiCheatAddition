package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import lombok.Getter;

import java.util.function.Function;

/**
 * This detects safe-walk behaviour (stopping when not sneaking)
 */
class SafewalkTypeTwoPattern implements Module
{
    @Getter
    private static final SafewalkTypeTwoPattern instance = new SafewalkTypeTwoPattern();

    @LoadFromConfiguration(configPath = ".violation_threshold")
    private int violationThreshold;

    @Getter
    private Function<User, Integer> applyingConsumer = user -> 0;

    @Override
    public void enable()
    {
        applyingConsumer = user -> {
            // Moved recently
            if (user.hasMovedRecently(TimestampKey.LAST_XZ_MOVEMENT, 355) &&
                // Suddenly stopped
                !user.hasMovedRecently(TimestampKey.LAST_XZ_MOVEMENT, 175) &&
                // Has not sneaked recently
                !(user.hasSneakedRecently(175) && user.getDataMap().getLong(DataKey.LAST_SNEAK_DURATION) > 148))
            {
                if (++user.getScaffoldData().safewalkTypeTwoFails >= this.violationThreshold) {
                    VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " has behaviour associated with safe-walk. (Type 2)");
                    return 4;
                }
            } else if (user.getScaffoldData().safewalkTypeTwoFails > 0) {
                user.getScaffoldData().safewalkTypeTwoFails--;
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
    public boolean isSubModule()
    {
        return true;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.safewalk.type2";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }
}

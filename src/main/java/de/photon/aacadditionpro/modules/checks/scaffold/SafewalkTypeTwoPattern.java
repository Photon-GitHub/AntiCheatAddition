package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This detects safe-walk behaviour (stopping when not sneaking)
 */
class SafewalkTypeTwoPattern extends PatternModule.Pattern<User, BlockPlaceEvent>
{
    @LoadFromConfiguration(configPath = ".violation_threshold")
    private int violationThreshold;

    @Override
    protected int process(User user, BlockPlaceEvent event)
    {
        // Moved recently
        if (user.hasMovedRecently(TimestampKey.LAST_XZ_MOVEMENT, 355) &&
            // Suddenly stopped
            !user.hasMovedRecently(TimestampKey.LAST_XZ_MOVEMENT, 175) &&
            // Has not sneaked recently
            !(user.hasSneakedRecently(175) && user.getDataMap().getLong(DataKey.LAST_SNEAK_DURATION) > 148))
        {
            if (++user.getScaffoldData().safewalkTypeTwoFails >= this.violationThreshold) {
                message = "Scaffold-Verbose | Player: " + user.getPlayer().getName() + " has behaviour associated with safe-walk. (Type 2)";
                return 4;
            }
        } else if (user.getScaffoldData().safewalkTypeTwoFails > 0) {
            user.getScaffoldData().safewalkTypeTwoFails--;
        }
        return 0;
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

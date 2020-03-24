package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.olduser.UserOld;
import de.photon.aacadditionpro.olduser.data.PositionDataOld;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This detects safe-walk behaviour (stopping when not sneaking)
 */
class SafewalkTypeTwoPattern extends PatternModule.Pattern<UserOld, BlockPlaceEvent>
{
    @LoadFromConfiguration(configPath = ".violation_threshold")
    private int violationThreshold;

    @Override
    protected int process(UserOld user, BlockPlaceEvent event)
    {
        // Moved recently
        if (user.getPositionData().hasPlayerMovedRecently(355, PositionDataOld.MovementType.XZONLY) &&
            // Suddenly stopped
            !user.getPositionData().hasPlayerMovedRecently(175, PositionDataOld.MovementType.XZONLY) &&
            // Has not sneaked recently
            !(user.getPositionData().hasPlayerSneakedRecently(175) && user.getPositionData().getLastSneakTime() > 148))
        {
            if (++user.getScaffoldData().safewalkTypeTwoFails >= this.violationThreshold) {
                message = "Scaffold-Verbose | Player: " + user.getPlayer().getName() + " has behaviour associated with safe-walk. (Type 2)";
                return 4;
            }
        }
        else if (user.getScaffoldData().safewalkTypeTwoFails > 0) {
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

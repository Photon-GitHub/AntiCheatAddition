package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.PositionData;
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
        if (user.getPositionData().hasPlayerMovedRecently(355, PositionData.MovementType.XZONLY) &&
            // Suddenly stopped
            !user.getPositionData().hasPlayerMovedRecently(175, PositionData.MovementType.XZONLY) &&
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

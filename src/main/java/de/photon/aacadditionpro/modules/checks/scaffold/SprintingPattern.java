package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.olduser.UserOld;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This pattern detects bursts of sprinting while scaffolding. No legit is able to properly utilize sprinting so far
 * because of the direction limitations.
 */
class SprintingPattern extends PatternModule.Pattern<UserOld, BlockPlaceEvent>
{
    @LoadFromConfiguration(configPath = ".violation_threshold")
    private int violationThreshold;

    @Override
    public int process(UserOld user, BlockPlaceEvent event)
    {
        if (user.getPositionData().hasPlayerSprintedRecently(400)) {
            if (++user.getScaffoldData().sprintingFails >= this.violationThreshold) {
                message = "Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sprinted suspiciously.";
                // Flag the player
                return 8;
            }
        }
        else if (user.getScaffoldData().sprintingFails > 0) {
            user.getScaffoldData().sprintingFails--;
        }

        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.sprinting";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }
}

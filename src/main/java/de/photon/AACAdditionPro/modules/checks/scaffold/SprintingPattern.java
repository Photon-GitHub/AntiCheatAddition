package de.photon.AACAdditionPro.modules.checks.scaffold;

import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This pattern detects bursts of sprinting while scaffolding. No legit is able to properly utilize sprinting so far
 * because of the direction limitations.
 */
class SprintingPattern extends PatternModule.Pattern<User, BlockPlaceEvent>
{
    @LoadFromConfiguration(configPath = ".violation_threshold")
    private int violation_threshold;

    @Override
    public int process(User user, BlockPlaceEvent event)
    {
        if (user.getPositionData().hasPlayerSprintedRecently(400))
        {
            if (++user.getScaffoldData().sprintingFails > this.violation_threshold)
            {
                VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sprinted suspiciously.");
                // Flag the player
                return 8;
            }
        }
        else if (user.getScaffoldData().sprintingFails > 0)
        {
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

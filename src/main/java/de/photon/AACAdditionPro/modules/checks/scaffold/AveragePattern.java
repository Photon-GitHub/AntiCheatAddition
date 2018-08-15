package de.photon.AACAdditionPro.modules.checks.scaffold;

import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.VerboseSender;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This {@link de.photon.AACAdditionPro.modules.PatternModule.Pattern} checks the average time between block places.
 */
class AveragePattern extends PatternModule.Pattern<User, BlockPlaceEvent>
{
    @Override
    public int process(User user, BlockPlaceEvent event)
    {
        // Should check average?
        if (user.getScaffoldData().getScaffoldBlockPlaces().hasReachedBufferSize())
        {
            /*
            Indices:
            [0] -> Expected time
            [1] -> Real time
             */
            final double[] results = user.getScaffoldData().calculateTimes();

            // delta-times are too low -> flag
            if (results[1] < results[0])
            {
                // Calculate the vl
                final int vlIncrease = (int) (4 * Math.min(Math.ceil((results[0] - results[1]) / 15D), 6));

                VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " enforced delay: " + results[0] + " | real: " + results[1] + " | vl increase: " + vlIncrease);
                return vlIncrease;
            }
        }

        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.average";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }
}

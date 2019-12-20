package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.datawrappers.ScaffoldBlockPlace;
import de.photon.aacadditionpro.util.entity.PotionUtil;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.potion.PotionEffectType;

/**
 * This {@link de.photon.aacadditionpro.modules.PatternModule.Pattern} checks the average time between block places.
 */
class AveragePattern extends PatternModule.Pattern<User, BlockPlaceEvent>
{
    @Override
    public int process(User user, BlockPlaceEvent event)
    {
        // Should check average?
        // Buffer the ScaffoldBlockPlace
        if (user.getScaffoldData().getScaffoldBlockPlaces().bufferObject(new ScaffoldBlockPlace(
                event.getBlockPlaced(),
                event.getBlockPlaced().getFace(event.getBlockAgainst()),
                // Speed-Effect
                PotionUtil.getAmplifier(PotionUtil.getPotionEffect(user.getPlayer(), PotionEffectType.SPEED)),
                user.getPlayer().getLocation().getYaw(),
                user.getPositionData().hasPlayerSneakedRecently(175))))
        {
            /*
            Indices:
            [0] -> Expected time
            [1] -> Real time
             */
            final double[] results = user.getScaffoldData().calculateTimes();

            // delta-times are too low -> flag
            if (results[1] < results[0]) {
                // Calculate the vl
                final int vlIncrease = (int) (4 * Math.min(Math.ceil((results[0] - results[1]) / 15D), 6));

                message = "Scaffold-Verbose | Player: " + user.getPlayer().getName() + " enforced delay: " + results[0] + " | real: " + results[1] + " | vl increase: " + vlIncrease;
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

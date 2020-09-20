package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.BatchProcessorModule;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.user.subdata.datawrappers.ScaffoldBlockPlace;
import de.photon.aacadditionpro.util.datastructures.batch.BatchProcessor;
import de.photon.aacadditionpro.util.potion.InternalPotionEffectType;
import de.photon.aacadditionpro.util.potion.PotionUtil;
import de.photon.aacadditionpro.util.world.BlockUtils;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This Module checks the average time between block places.
 */
public class AveragePattern implements ListenerModule, BatchProcessorModule<ScaffoldBlockPlace>
{
    @Getter
    private static final AveragePattern instance = new AveragePattern();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // Ladders and Vines are prone to false positives as they can be used to place blocks immediately after placing
        // them, therefore almost doubling the placement speed. However they can only be placed one at a time, which
        // allows simply ignoring them.
        if (event.getBlockPlaced().getType() != Material.LADDER && event.getBlockPlaced().getType() != Material.VINE) {
            if (!BlockUtils.isNext(user.getScaffoldData().getScaffoldBlockPlaces().peekLastAdded().getBlock(), event.getBlockPlaced(), true)) {
                user.getScaffoldData().getScaffoldBlockPlaces().clear();
            }

            user.getScaffoldData().getScaffoldBlockPlaces().addDataPoint(new ScaffoldBlockPlace(
                    event.getBlockPlaced(),
                    event.getBlockPlaced().getFace(event.getBlockAgainst()),
                    // Speed-Effect
                    PotionUtil.getAmplifier(PotionUtil.getPotionEffect(user.getPlayer(), InternalPotionEffectType.SPEED)),
                    user.getPlayer().getLocation().getYaw(),
                    user.hasSneakedRecently(175)));
        }
    }

    @Override
    public void enable()
    {
        AverageBatchProcessor.getInstance().enable();
    }

    @Override
    public void disable()
    {
        AverageBatchProcessor.getInstance().disable();
    }

    @Override
    public boolean isSubModule()
    {
        return true;
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

    @Override
    public BatchProcessor<ScaffoldBlockPlace> getBatchProcessor()
    {
        return AverageBatchProcessor.getInstance();
    }
}

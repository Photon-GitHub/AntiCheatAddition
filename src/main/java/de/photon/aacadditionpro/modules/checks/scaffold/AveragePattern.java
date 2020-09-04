package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.BatchProcessorModule;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.user.subdata.ScaffoldData;
import de.photon.aacadditionpro.user.subdata.datawrappers.ScaffoldBlockPlace;
import de.photon.aacadditionpro.util.datastructures.batch.BatchProcessor;
import de.photon.aacadditionpro.util.datastructures.iteration.IterationUtil;
import de.photon.aacadditionpro.util.inventory.InventoryUtils;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import de.photon.aacadditionpro.util.potion.InternalPotionEffectType;
import de.photon.aacadditionpro.util.potion.PotionUtil;
import de.photon.aacadditionpro.util.world.BlockUtils;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;

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
            if (BlockUtils.isNext(user.getScaffoldData().getScaffoldBlockPlaces().peekLastAdded().getBlock(), event.getBlockPlaced(), true)) {
                user.getScaffoldData().getScaffoldBlockPlaces().addDataPoint(new ScaffoldBlockPlace(
                        event.getBlockPlaced(),
                        event.getBlockPlaced().getFace(event.getBlockAgainst()),
                        // Speed-Effect
                        PotionUtil.getAmplifier(PotionUtil.getPotionEffect(user.getPlayer(), InternalPotionEffectType.SPEED)),
                        user.getPlayer().getLocation().getYaw(),
                        user.hasSneakedRecently(175)));
            } else {
                user.getScaffoldData().getScaffoldBlockPlaces().clear();
            }
        }
    }

    @Override
    public void enable()
    {
        AverageBatchProcessor.getInstance().startProcessing();
    }

    @Override
    public void disable()
    {
        AverageBatchProcessor.getInstance().killProcessing();
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

    private static class AverageBatchProcessor extends BatchProcessor<ScaffoldBlockPlace>
    {
        @Getter
        private static final AverageBatchProcessor instance = new AverageBatchProcessor();

        private AverageBatchProcessor()
        {
            super(ScaffoldData.BATCH_SIZE);
        }

        @Override
        public void processBatch(User user, List<ScaffoldBlockPlace> batch)
        {
            /*
             * Used to calculate the average and expected time span between the ScaffoldBlockPlaces in the buffer.
             * Also clears the buffer.
             *
             * @return an array with the following contents:<br>
             * [0] = Expected time <br>
             * [1] = Real time <br>
             */
            final double[] result = new double[2];

            // -1 because there is one pop to fill the "last" variable in the beginning.
            final int divisor = batch.size() - 1;

            final boolean moonwalk = batch.stream().filter(blockPlace -> !blockPlace.isSneaked()).count() >= ScaffoldData.BATCH_SIZE / 2;

            IterationUtil.twoObjectsIterationToEnd(batch, (old, current) -> {
                double delay;
                if (current.getBlockFace() == old.getBlockFace() || current.getBlockFace() == old.getBlockFace().getOppositeFace()) {
                    delay = ScaffoldData.DELAY_NORMAL;

                    if (!moonwalk && current.isSneaked() && old.isSneaked()) {
                        delay += ScaffoldData.SNEAKING_ADDITION + (ScaffoldData.SNEAKING_SLOW_ADDITION * Math.abs(Math.cos(2D * current.getYaw())));
                    }
                } else {
                    delay = ScaffoldData.DELAY_DIAGONAL;
                }

                result[0] += delay;

                // last - current to calculate the delta as the more recent time is always in last.
                result[1] += (current.getTime() - old.getTime()) * old.getSpeedModifier();
            });

            result[0] /= divisor;
            result[1] /= divisor;

            // delta-times are too low -> flag
            if (result[1] < result[0]) {
                // Calculate the vl
                final int vlIncrease = (int) (4 * Math.min(Math.ceil((result[0] - result[1]) / 15D), 6));
                Scaffold.getInstance().getViolationLevelManagement().flag(user.getPlayer(), vlIncrease, Scaffold.getInstance().getCancelVl(), () -> {
                    user.getTimestampMap().updateTimeStamp(TimestampKey.SCAFFOLD_TIMEOUT);
                    InventoryUtils.syncUpdateInventory(user.getPlayer());
                }, () -> VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " enforced delay: " + result[0] + " | real: " + result[1] + " | vl increase: " + vlIncrease));
            }
        }
    }
}

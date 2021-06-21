package de.photon.aacadditionpro.modules.checks.scaffold;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.user.data.batch.ScaffoldBatch;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.datastructure.batch.AsyncBatchProcessor;
import de.photon.aacadditionpro.util.datastructure.batch.BatchPreprocessors;
import de.photon.aacadditionpro.util.datastructure.statistics.DoubleStatistics;
import de.photon.aacadditionpro.util.inventory.InventoryUtil;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import lombok.val;

import java.util.List;
import java.util.LongSummaryStatistics;

class ScaffoldAverageBatchProcessor extends AsyncBatchProcessor<ScaffoldBatch.ScaffoldBlockPlace>
{
    private final int cancelVl = AACAdditionPro.getInstance().getConfig().getInt(this.getModule().getConfigString() + ".cancel_vl");

    @LoadFromConfiguration(configPath = ".delays.normal")
    public double normalDelay;
    @LoadFromConfiguration(configPath = ".delays.sneaking_addition")
    public double sneakingAddition;
    @LoadFromConfiguration(configPath = ".delays.sneaking_addition")
    public double sneakingSlowAddition;
    @LoadFromConfiguration(configPath = ".delays.diagonal")
    public double diagonalDelay;

    ScaffoldAverageBatchProcessor(ViolationModule module)
    {
        super(module, ImmutableSet.of(ScaffoldBatch.SCAFFOLD_BATCH_BROADCASTER));
    }

    @Override
    public void processBatch(User user, List<ScaffoldBatch.ScaffoldBlockPlace> batch)
    {
        val moonwalk = batch.stream().filter(blockPlace -> !blockPlace.isSneaked()).count() >= batch.size() / 2;
        val actualDelay = new LongSummaryStatistics();
        val minExpecedDelay = new DoubleStatistics();

        for (val pair : BatchPreprocessors.zipOffsetOne(batch)) {
            actualDelay.accept(pair.getSecond().timeOffset(pair.getFirst()));

            if (pair.getSecond().getBlockFace() == pair.getFirst().getBlockFace() || pair.getSecond().getBlockFace() == pair.getFirst().getBlockFace().getOppositeFace()) {
                // Sneaking handling
                if (!moonwalk && pair.getSecond().isSneaked() && pair.getFirst().isSneaked())
                    minExpecedDelay.accept(normalDelay + sneakingAddition + (sneakingSlowAddition * Math.abs(Math.cos(2D * pair.getSecond().getYaw()))));
                    // Moonwalking.
                else minExpecedDelay.accept(normalDelay);
                // Not the same blockfaces means that something is built diagonally or a new build position which means higher actual delay anyways and can be ignored.
            } else minExpecedDelay.accept(diagonalDelay);

            val actualAverage = actualDelay.getAverage();
            val minExpecedAverage = minExpecedDelay.getAverage();

            // delta-times are too low -> flag
            if (actualAverage < minExpecedAverage) {
                val vlIncrease = (int) (4 * Math.min(Math.ceil((minExpecedAverage - actualAverage) / 15D), 6));
                this.getModule().getManagement().flag(Flag.of(user)
                                                          .setAddedVl(vlIncrease)
                                                          .setCancelAction(cancelVl, () -> {
                                                              user.getTimestampMap().at(TimestampKey.SCAFFOLD_TIMEOUT).update();
                                                              InventoryUtil.syncUpdateInventory(user.getPlayer());
                                                          })
                                                          .setEventNotCancelledAction(() -> DebugSender.getInstance().sendDebug("Scaffold-Debug | Player: " + user.getPlayer().getName() +
                                                                                                                                " enforced delay: " + minExpecedAverage + " | real: " + actualAverage +
                                                                                                                                " | vl increase: " + vlIncrease)));
            }
        }
    }
}

package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.user.data.batch.ScaffoldBatch;
import de.photon.aacadditionpro.util.datastructure.batch.AsyncBatchProcessor;
import de.photon.aacadditionpro.util.datastructure.batch.BatchPreprocessors;
import de.photon.aacadditionpro.util.inventory.InventoryUtil;
import de.photon.aacadditionpro.util.mathematics.Polynomial;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import lombok.val;

import java.util.List;
import java.util.Set;

class ScaffoldAverageBatchProcessor extends AsyncBatchProcessor<ScaffoldBatch.ScaffoldBlockPlace>
{
    private static final Polynomial VL_CALCULATOR = new Polynomial(1.2222222, 20);
    public final double normalDelay = AACAdditionPro.getInstance().getConfig().getDouble(this.getModule().getConfigString() + ".parts.Average.delays.normal");
    public final double sneakingAddition = AACAdditionPro.getInstance().getConfig().getDouble(this.getModule().getConfigString() + ".parts.Average.delays.sneaking_addition");
    public final double sneakingSlowAddition = AACAdditionPro.getInstance().getConfig().getDouble(this.getModule().getConfigString() + ".parts.Average.delays.sneaking_slow_addition");
    public final double diagonalDelay = AACAdditionPro.getInstance().getConfig().getDouble(this.getModule().getConfigString() + ".parts.Average.delays.diagonal");
    private final int cancelVl = AACAdditionPro.getInstance().getConfig().getInt(this.getModule().getConfigString() + ".cancel_vl");

    ScaffoldAverageBatchProcessor(ViolationModule module)
    {
        super(module, Set.of(ScaffoldBatch.SCAFFOLD_BATCH_BROADCASTER));
    }

    @Override
    public void processBatch(User user, List<ScaffoldBatch.ScaffoldBlockPlace> batch)
    {
        val moonwalk = batch.stream().filter(blockPlace -> !blockPlace.isSneaked()).count() >= batch.size() / 2;
        val delays = BatchPreprocessors.zipReduceToDoubleStatistics(batch,
                                                                    (old, cur) -> old.getSpeedModifier() * cur.timeOffset(old),
                                                                    (old, cur) -> calculateMinExpectedDelay(old, cur, moonwalk));

        val actualAverage = delays.get(0).getAverage();
        val minExpectedAverage = delays.get(1).getAverage();

        // delta-times are too low -> flag
        if (actualAverage < minExpectedAverage) {
            val vlIncrease = Math.min(130, VL_CALCULATOR.apply(minExpectedAverage - actualAverage).intValue());
            this.getModule().getManagement().flag(Flag.of(user)
                                                      .setAddedVl(vlIncrease)
                                                      .setCancelAction(cancelVl, () -> {
                                                          user.getTimestampMap().at(TimestampKey.SCAFFOLD_TIMEOUT).update();
                                                          InventoryUtil.syncUpdateInventory(user.getPlayer());
                                                      })
                                                      .setEventNotCancelledAction(() -> DebugSender.getInstance().sendDebug("Scaffold-Debug | Player: " + user.getPlayer().getName() +
                                                                                                                            " enforced delay: " + minExpectedAverage + " | real: " + actualAverage +
                                                                                                                            " | vl increase: " + vlIncrease)));
        }
    }

    private double calculateMinExpectedDelay(ScaffoldBatch.ScaffoldBlockPlace old, ScaffoldBatch.ScaffoldBlockPlace current, boolean moonwalk)
    {
        if (current.getBlockFace() == old.getBlockFace() || current.getBlockFace() == old.getBlockFace().getOppositeFace()) {
            return !moonwalk && current.isSneaked() && old.isSneaked() ?
                   // Sneaking handling
                   normalDelay + sneakingAddition + (sneakingSlowAddition * Math.abs(Math.cos(2D * current.getLocation().getYaw()))) :
                   // Moonwalking.
                   normalDelay;
            // Not the same blockfaces means that something is built diagonally or a new build position which means higher actual delay anyways and can be ignored.
        } else return diagonalDelay;
    }
}

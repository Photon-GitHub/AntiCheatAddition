package de.photon.aacadditionpro.modules.checks.inventory;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.user.data.batch.InventoryBatch;
import de.photon.aacadditionpro.util.datastructure.batch.AsyncBatchProcessor;
import de.photon.aacadditionpro.util.datastructure.batch.BatchPreprocessors;
import de.photon.aacadditionpro.util.mathematics.DataUtil;
import de.photon.aacadditionpro.util.mathematics.Polynomial;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import lombok.val;

import java.util.List;

public class AverageHeuristicBatchProcessor extends AsyncBatchProcessor<InventoryBatch.InventoryClick>
{
    private static final Polynomial AVERAGE_MULTIPLIER_CALCULATOR = new Polynomial(-0.000102881, 0.00709709, -0.171127, 1.65);

    protected AverageHeuristicBatchProcessor(ViolationModule module)
    {
        super(module, ImmutableSet.of(InventoryBatch.INVENTORY_BATCH_BROADCASTER));
    }

    @Override
    public void processBatch(User user, List<InventoryBatch.InventoryClick> batch)
    {
        val timeOffsets = BatchPreprocessors.zipOffsetOne(batch).stream()
                                            .filter(pair -> pair.getFirst().getInventory().equals(pair.getSecond().getInventory()))
                                            .mapToLong(pair -> pair.getFirst().timeOffset(pair.getSecond()))
                                            .toArray();


        val misClickCounter = user.getDataMap().getCounter(DataKey.CounterKey.INVENTORY_AVERAGE_HEURISTICS_MISCLICKS);

        // Not enough data to check as the player opened many different inventories.
        if (timeOffsets.length < 8) {
            misClickCounter.setToZero();
            return;
        }

        val averageMillis = DataUtil.average(timeOffsets);
        val squaredErrorsSum = DataUtil.squaredError(averageMillis, timeOffsets);

        // One time 2 ticks offset and 2 times 1 tick offset * 15 minimum vl = 168750
        // 2500 error sum is legit achievable.
        // +1 to avoid division by 0
        double vl = 40000 / (squaredErrorsSum + 1);

        // Average below 1 tick is considered inhuman and increases vl.
        val averageMultiplier = AVERAGE_MULTIPLIER_CALCULATOR.apply(averageMillis / 50);
        vl *= Math.max(averageMultiplier, 0.5);

        // Make sure that misclicks are applied correctly.
        vl /= (misClickCounter.getCounter() + 1);

        // Mitigation for possibly better players.
        vl -= 10;

        // Too low vl.
        if (vl < 10) return;

        val finalVl = (int) Math.min(vl, 35);
        this.getModule().getManagement().flag(Flag.of(user)
                                                  .setAddedVl(finalVl)
                                                  .setEventNotCancelledAction(() -> DebugSender.getInstance().sendDebug("Inventory-Debug | Player: " + user.getPlayer() + " has bot-like click delays. (SE: " + squaredErrorsSum + " | A: " + averageMillis + " | MC: " + misClickCounter.getCounter() + " | VLU: " + finalVl + ")")));

        misClickCounter.setToZero();
    }
}
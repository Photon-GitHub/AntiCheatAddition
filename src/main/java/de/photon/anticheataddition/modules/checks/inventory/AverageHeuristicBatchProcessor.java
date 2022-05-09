package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataKey;
import de.photon.anticheataddition.user.data.batch.InventoryBatch;
import de.photon.anticheataddition.util.datastructure.batch.AsyncBatchProcessor;
import de.photon.anticheataddition.util.datastructure.batch.BatchPreprocessors;
import de.photon.anticheataddition.util.mathematics.DataUtil;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import de.photon.anticheataddition.util.violationlevels.Flag;
import lombok.val;

import java.util.List;
import java.util.Set;

public final class AverageHeuristicBatchProcessor extends AsyncBatchProcessor<InventoryBatch.InventoryClick>
{
    private static final Polynomial AVERAGE_MULTIPLIER_CALCULATOR = new Polynomial(-0.000205762, 0.0141942, -0.342254, 3.3);

    AverageHeuristicBatchProcessor(ViolationModule module)
    {
        super(module, Set.of(InventoryBatch.INVENTORY_BATCH_EVENTBUS));
    }

    @Override
    public void processBatch(User user, List<InventoryBatch.InventoryClick> batch)
    {
        val timeOffsets = BatchPreprocessors.zipOffsetOne(batch).stream()
                                            .filter(pair -> pair.first().getInventory().equals(pair.second().getInventory()))
                                            .mapToLong(pair -> pair.first().timeOffset(pair.second()))
                                            .toArray();


        val misClickCounter = user.getDataMap().getCounter(DataKey.Count.INVENTORY_AVERAGE_HEURISTICS_MISCLICKS);

        // Not enough data to check as the player opened many inventories.
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
        // / 50 to make sure the coefficients are big enough to avoid precision bugs.
        vl *= Math.max(AVERAGE_MULTIPLIER_CALCULATOR.apply(averageMillis / 50), 0.5);

        // Make sure that misclicks are applied correctly.
        vl /= (misClickCounter.getCounter() + 1);

        // Mitigation for possibly better players.
        vl -= 10;

        // Too low vl.
        if (vl < 10) return;

        val finalVl = (int) Math.min(vl, 70);
        this.getModule().getManagement().flag(Flag.of(user)
                                                  .setAddedVl(finalVl)
                                                  .setDebug(() -> "Inventory-Debug | Player: " + user.getPlayer().getName() +
                                                                  " has bot-like click delays. (SE: " + squaredErrorsSum + " | A: " + averageMillis + " | MC: " + misClickCounter.getCounter() + " | VLU: " + finalVl + ")"));

        misClickCounter.setToZero();
    }
}
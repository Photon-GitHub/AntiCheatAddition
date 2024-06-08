package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.ViolationCounter;
import de.photon.anticheataddition.user.data.batch.InventoryBatch;
import de.photon.anticheataddition.util.datastructure.batch.AsyncBatchProcessor;
import de.photon.anticheataddition.util.datastructure.batch.BatchPreprocessors;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.mathematics.DataUtil;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import de.photon.anticheataddition.util.violationlevels.Flag;

import java.util.Arrays;
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
        if (User.isUserInvalid(user, this.getModule())) return;

        final long[] timeOffsets = BatchPreprocessors.zipOffsetOne(batch).stream()
                                                     .filter(pair -> pair.first().inventory().equals(pair.second().inventory()))
                                                     .mapToLong(pair -> pair.first().timeOffset(pair.second()))
                                                     .toArray();


        final var misClickCounter = user.getData().counter.inventoryAverageHeuristicsMisclicks;

        // Not enough data to check as the player opened many inventories.
        if (timeOffsets.length < 8) {
            misClickCounter.setToZero();
            return;
        }

        varianceTest(user, timeOffsets, misClickCounter);
    }

    private void varianceTest(User user, long[] timeOffsets, ViolationCounter misClickCounter)
    {
        Log.finer(() -> "Inventory-Debug | Player: %s | Average-Heuristics | Raw: %s".formatted(user.getPlayer().getName(), Arrays.toString(timeOffsets)));

        // Remove a single outlier that often happens after opening a new inventory.
        final long[] timeOffsetsOutlierRemoved = DataUtil.removeOutliers(1, timeOffsets);

        final double averageMillis = DataUtil.average(timeOffsetsOutlierRemoved);
        final double variance = DataUtil.variance(averageMillis, timeOffsetsOutlierRemoved);

        // One time 2 ticks offset and 2 times 1 tick offset * 15 minimum vl = 168750
        // 2500 error sum is legit achievable.
        // +1 to avoid division by 0
        final int vl = getVl(misClickCounter, variance, averageMillis);

        Log.finer(() -> "Inventory-Debug | Player: %s | Average-Heuristics | (VAR: %f | AVG: %f | MISS: %d | VL: %d)".formatted(user.getPlayer().getName(), variance, averageMillis, misClickCounter.getCounter(), vl));

        // Too low vl.
        if (vl < 10) return;

        final int finalVl = Math.min(vl, 70);
        this.getModule().getManagement().flag(Flag.of(user)
                                                  .setAddedVl(finalVl)
                                                  .setDebug(() -> "Inventory-Debug | Player: %s has constant click delays. (VAR: %f | AVG: %f | MISS: %d | VL: %d)".formatted(user.getPlayer().getName(), variance, averageMillis, misClickCounter.getCounter(), finalVl)));

        misClickCounter.setToZero();
    }

    private static int getVl(ViolationCounter misClickCounter, double variance, double averageMillis)
    {
        double vl = 30000 / (variance + 1);

        // Average below 1 tick is considered inhuman and increases vl.
        // / 50 to make sure the coefficients are big enough to avoid precision bugs.
        vl *= Math.max(AVERAGE_MULTIPLIER_CALCULATOR.apply(averageMillis / 50), 0.5);

        // Make sure that misclicks are applied correctly.
        vl /= (misClickCounter.getCounter() + 1);

        // Mitigation for possibly better players.
        vl -= 15;
        return (int) vl;
    }
}
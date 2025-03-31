package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.ViolationCounter;
import de.photon.anticheataddition.user.data.batch.InventoryBatch;
import de.photon.anticheataddition.util.datastructure.batch.BatchPreprocessors;
import de.photon.anticheataddition.util.datastructure.batch.SyncBatchProcessor;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.mathematics.DataUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class AverageHeuristicBatchProcessor extends SyncBatchProcessor<InventoryBatch.InventoryClick>
{
    AverageHeuristicBatchProcessor(ViolationModule module)
    {
        super(module, Set.of(InventoryBatch.INVENTORY_BATCH_EVENTBUS));
    }

    @Override
    public void processBatch(User user, List<InventoryBatch.InventoryClick> batch)
    {
        if (User.isUserInvalid(user, this.getModule())) return;

        final long[] timeOffsets = BatchPreprocessors.zipOffsetOne(batch).stream()
                                                     // Same inventory, ignore pairs with different inventories.
                                                     .filter(pair -> pair.first().inventory().equals(pair.second().inventory()))
                                                     // Calculate the time offset between the two clicks.
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
        final long[] timeOffsetsOutlierRemoved = DataUtil.removeOutliers(2, timeOffsets);

        final double averageMillis = DataUtil.average(timeOffsetsOutlierRemoved);
        final double variance = DataUtil.variance(averageMillis, timeOffsetsOutlierRemoved);

        // One time 2 ticks offset and 2 times 1 tick offset * 15 minimum vl = 168750
        // 2500 error sum is legit achievable.
        // +1 to avoid division by 0
        final int vl = getVl(misClickCounter, variance, averageMillis);

        Log.finer(() -> "Inventory-Debug | Player: %s | Average-Heuristics | (VAR: %f | AVG: %f | MISS: %d | VL: %d)".formatted(user.getPlayer().getName(), variance, averageMillis, misClickCounter.getCounter(), vl));

        // Too low vl.
        if (vl <= 0) return;

        final int finalVl = Math.min(vl, 70);
        this.getModule().getManagement().flag(Flag.of(user)
                                                  .setAddedVl(finalVl)
                                                  .setDebug(() -> "Inventory-Debug | Player: %s has constant click delays. (VAR: %f | AVG: %f | MISS: %d | VL: %d)".formatted(user.getPlayer().getName(), variance, averageMillis, misClickCounter.getCounter(), finalVl)));

        misClickCounter.setToZero();
    }

    private static int getVl(ViolationCounter misClickCounter, double variance, double averageMillis)
    {
        double vl = 40000 / (variance + 1);

        // Average below 1 tick is considered inhuman and increases vl.
        // / 50 to make sure the coefficients are big enough to avoid precision bugs.
        vl *= averageMultiplier(averageMillis / 50);

        // Make sure that misclicks are applied correctly.
        vl /= (misClickCounter.getCounter() + 1);

        // Mitigation for possibly better players.
        vl -= 25;
        return (int) vl;
    }

    private static double averageMultiplier(double averageTicks)
    {
        // This function assumes that large average values are more likely to be legit: lim x-> inf: f(x) = 0.5
        // and models how the vl should increase with lower average values: https://www.wolframalpha.com/input?i=plot+-1.3*tanh%280.15x-1%29+%2B+1.8
        return -1.3 * Math.tanh(0.15 * averageTicks - 1) + 1.8;
    }
}
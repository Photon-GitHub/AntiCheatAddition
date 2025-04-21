package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.batch.InventoryBatch;
import de.photon.anticheataddition.util.datastructure.batch.BatchPreprocessors;
import de.photon.anticheataddition.util.datastructure.batch.SyncBatchProcessor;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.mathematics.DataUtil;
import de.photon.anticheataddition.util.mathematics.KolmogorovSmirnov;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import de.photon.anticheataddition.util.violationlevels.Flag;

import java.util.List;
import java.util.Set;

/**
 * The StatisticalBatchProcessor class processes batches of inventory clicks from users,
 * analyzing the time offsets between clicks to detect suspicious behavior using the Kolmogorov-Smirnov test.
 */
public final class StatisticalBatchProcessor extends SyncBatchProcessor<InventoryBatch.InventoryClick>
{
    private static final double PROBABILITY_THRESHOLD = 0.5;
    // Polynomial for calculating violation levels based on the D-statistic.
    private static final Polynomial D_TEST_VL_CALCULATOR = new Polynomial(59, 1);

    /**
     * Constructor for StatisticalBatchProcessor.
     *
     * @param module The violation module to which this processor belongs.
     */
    StatisticalBatchProcessor(ViolationModule module)
    {
        super(module, Set.of(InventoryBatch.INVENTORY_BATCH_EVENTBUS));
    }

    /**
     * Processes a batch of inventory clicks.
     * If the user is invalid or there is not enough data, the method returns early.
     * Otherwise, it proceeds to perform a Kolmogorov-Smirnov test on the time offsets between clicks.
     *
     * @param user  The user whose inventory clicks are being processed.
     * @param batch The batch of inventory clicks to process.
     */
    @Override
    public void processBatch(User user, List<InventoryBatch.InventoryClick> batch)
    {
        if (User.isUserInvalid(user, this.getModule())) return;

        // Calculate time offsets between successive clicks on the same inventory.
        final long[] timeOffsets = BatchPreprocessors.zipOffsetOne(batch).stream()
                                                     .filter(pair -> pair.first().inventory().equals(pair.second().inventory()))
                                                     .mapToLong(pair -> pair.first().timeOffset(pair.second()))
                                                     .toArray();

        Log.finest(() -> "Inventory-Debug | Statistical Player: %s LEN: %d".formatted(user.getPlayer().getName(), timeOffsets.length));

        // Not enough data to check as the player opened many inventories.
        if (timeOffsets.length < 10) return;

        // Remove outliers that might mask the distribution.
        final long[] timeOffsetsOutliersRemoved = DataUtil.removeOutliers(2, timeOffsets);
        final KolmogorovSmirnov.KsResult result = KolmogorovSmirnov.uniformTest(timeOffsetsOutliersRemoved);

        Log.finer(() -> "Inventory-Debug | Statistical Player: %s, p-value: %f, p-threshold: %f".formatted(user.getPlayer().getName(), result.pValue(), PROBABILITY_THRESHOLD));

        // If the p-value is above the threshold, we are reasonably sure that the distribution is uniform.
        if (result.pValue() < PROBABILITY_THRESHOLD) return;
        this.getModule().getManagement().flag(Flag.of(user)
                                                  .setAddedVl(D_TEST_VL_CALCULATOR.apply(result.pValue()).intValue())
                                                  .setDebug(() -> "Inventory-Debug | Player: %s has suspiciously distributed click delays. (p-value: %f, p-threshold: %f)".formatted(user.getPlayer().getName(), result.pValue(), PROBABILITY_THRESHOLD)));
    }
}

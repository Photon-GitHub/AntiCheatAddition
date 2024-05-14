package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.batch.InventoryBatch;
import de.photon.anticheataddition.util.datastructure.batch.AsyncBatchProcessor;
import de.photon.anticheataddition.util.datastructure.batch.BatchPreprocessors;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.mathematics.KolmogorovSmirnow;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import de.photon.anticheataddition.util.violationlevels.Flag;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * The StatisticalBatchProcessor class processes batches of inventory clicks from users,
 * analyzing the time offsets between clicks to detect suspicious behavior using the Kolmogorov-Smirnov test.
 */
public final class StatisticalBatchProcessor extends AsyncBatchProcessor<InventoryBatch.InventoryClick>
{
    // TODO: Refine this value further.
    // Threshold value for the Kolmogorov-Smirnov test (D-statistic).
    private static final double D_TEST = 0.21;
    // Polynomial for calculating violation levels based on the D-statistic.
    private static final Polynomial D_TEST_VL_CALCULATOR = new Polynomial(-50, 60);

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

        kolmogorowSmirnowTest(user, timeOffsets);
    }

    /**
     * Performs the Kolmogorov-Smirnov test on the given time offsets to detect suspicious behavior.
     *
     * @param user        The user whose inventory clicks are being analyzed.
     * @param timeOffsets The time offsets between successive inventory clicks.
     */
    private void kolmogorowSmirnowTest(User user, long[] timeOffsets)
    {
        // Normalize the clickOffsets to the [0, 1] range
        final double[] normalizedOffsets = KolmogorovSmirnow.normalizeData(timeOffsets);

        Log.finest(() -> "Inventory-Debug | Statistical Player: %s | RAW-OFFSET: %s | SCALED-OFFSET: %s".formatted(user.getPlayer().getName(), Arrays.toString(timeOffsets), Arrays.toString(normalizedOffsets)));

        // Perform the K-S test
        final double d_max = KolmogorovSmirnow.kolmogorovSmirnowUniformTest(normalizedOffsets);

        Log.finer(() -> "Inventory-Debug | Statistical Player: %s, D_MAX: %f, D_TEST: %f".formatted(user.getPlayer().getName(), d_max, D_TEST));

        // If the D-statistic is greater than or equal to the threshold, return (no uniform distribution found).
        if (d_max >= D_TEST) return;
        this.getModule().getManagement().flag(Flag.of(user)
                                                  .setAddedVl(D_TEST_VL_CALCULATOR.apply(d_max / D_TEST).intValue())
                                                  .setDebug(() -> "Inventory-Debug | Player: %s has suspiciously distributed click delays. (D_MAX: %f, D_TEST: %f)".formatted(user.getPlayer().getName(), d_max, D_TEST)));
    }
}

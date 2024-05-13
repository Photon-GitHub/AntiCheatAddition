package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.batch.InventoryBatch;
import de.photon.anticheataddition.util.datastructure.batch.AsyncBatchProcessor;
import de.photon.anticheataddition.util.datastructure.batch.BatchPreprocessors;
import de.photon.anticheataddition.util.mathematics.DataUtil;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.violationlevels.Flag;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public final class StatisticalBatchProcessor extends AsyncBatchProcessor<InventoryBatch.InventoryClick>
{
    // TODO: Refine this value further.
    private static final double D_TEST = 0.21;
    private static final Polynomial D_TEST_VL_CALCULATOR = new Polynomial(-50, 60);

    StatisticalBatchProcessor(ViolationModule module)
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

        Log.finest(() -> "Inventory-Debug | Statistical Player: %s LEN: %d".formatted(user.getPlayer().getName(), timeOffsets.length));

        // Not enough data to check as the player opened many inventories.
        if (timeOffsets.length < 10) return;

        kolmogorowSmirnowTest(user, timeOffsets);
    }

    private void kolmogorowSmirnowTest(User user, long[] timeOffsets)
    {
        final double average = DataUtil.average(timeOffsets);

        // Subtract the average to make sure that any base value on which a uniform distribution was added is removed.
        final double[] sortedCenterOffset = Arrays.stream(timeOffsets).mapToDouble(offset -> offset - average).sorted().toArray();
        // Scale to the range -0.5 to 0.5
        final double scalingFactor = 2 * MathUtil.absDiff(sortedCenterOffset[0], sortedCenterOffset[sortedCenterOffset.length - 1]);
        // Scale to the range 0 to 1
        final double startAtZeroFactor = sortedCenterOffset[0] / scalingFactor;

        final double[] scaledTimeOffsets = Arrays.stream(sortedCenterOffset).map(d -> d / scalingFactor).map(d -> d + startAtZeroFactor).toArray();

        Log.finest(() -> "Inventory-Debug | Statistical Player: %s | RAW-OFFSET: %s | SCALED-OFFSET: %s".formatted(user.getPlayer().getName(), Arrays.toString(sortedCenterOffset), Arrays.toString(scaledTimeOffsets)));

        double d_max = 0;
        for (int i = 0; i < scaledTimeOffsets.length; i++) {
            final double uniform = (i + 0.5) / scaledTimeOffsets.length;
            final double d = MathUtil.absDiff(scaledTimeOffsets[i], uniform);
            if (d > d_max) d_max = d;
        }

        final double finalD_max = d_max;

        Log.finer(() -> "Inventory-Debug | Statistical Player: %s SCALE: %f, START: %f, D_MAX: %f, D_TEST: %f".formatted(user.getPlayer(), scalingFactor, startAtZeroFactor, finalD_max, D_TEST));

        if (d_max >= D_TEST) return;
        this.getModule().getManagement().flag(Flag.of(user)
                                                  .setAddedVl(D_TEST_VL_CALCULATOR.apply(d_max / D_TEST).intValue())
                                                  .setDebug(() -> "Inventory-Debug | Player: %s has suspiciously distributed click delays. (SCALE: %f, START: %f, D_MAX: %f, D_TEST: %f)".formatted(user.getPlayer().getName(), scalingFactor, startAtZeroFactor, finalD_max, D_TEST)));
    }
}

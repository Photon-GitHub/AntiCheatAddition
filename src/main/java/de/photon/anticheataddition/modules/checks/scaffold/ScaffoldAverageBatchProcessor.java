package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.user.data.batch.ScaffoldBatch;
import de.photon.anticheataddition.util.datastructure.batch.AsyncBatchProcessor;
import de.photon.anticheataddition.util.datastructure.batch.BatchPreprocessors;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import de.photon.anticheataddition.util.messaging.Log;
import de.photon.anticheataddition.util.violationlevels.Flag;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class ScaffoldAverageBatchProcessor extends AsyncBatchProcessor<ScaffoldBatch.ScaffoldBlockPlace>
{
    private static final Polynomial VL_CALCULATOR = new Polynomial(1.2222222, 20);

    public final double normalDelay = loadDouble(".parts.Average.delays.normal", 238);
    public final double sneakingAddition = loadDouble(".parts.Average.delays.sneaking_addition", 70);
    public final double sneakingSlowAddition = loadDouble(".parts.Average.delays.sneaking_slow_addition", 100);
    public final double diagonalDelay = loadDouble(".parts.Average.delays.diagonal", 138);
    private final int cancelVl = loadInt(".cancel_vl", 110);

    ScaffoldAverageBatchProcessor(ViolationModule module)
    {
        super(module, Set.of(ScaffoldBatch.SCAFFOLD_BATCH_EVENTBUS));
    }

    @Override
    public void processBatch(User user, List<ScaffoldBatch.ScaffoldBlockPlace> batch)
    {
        final boolean moonwalk = batch.stream().filter(Predicate.not(ScaffoldBatch.ScaffoldBlockPlace::sneaked)).count() >= batch.size() / 2;
        final var delays = BatchPreprocessors.zipReduceToDoubleStatistics(batch,
                                                                          (old, cur) -> old.speedModifier() * cur.timeOffset(old),
                                                                          (old, cur) -> calculateMinExpectedDelay(old, cur, moonwalk));

        final double actualAverage = delays.get(0).getAverage();
        final double minExpectedAverage = delays.get(1).getAverage();

        Log.finer(() -> "Scaffold-Debug | Player: %s min delay: %f | real: %f".formatted(user.getPlayer().getName(), minExpectedAverage, actualAverage));

        // delta-times are too low -> flag
        if (actualAverage < minExpectedAverage) {
            final int vlIncrease = Math.min(130, VL_CALCULATOR.apply(minExpectedAverage - actualAverage).intValue());
            this.getModule().getManagement().flag(Flag.of(user)
                                                      .setAddedVl(vlIncrease)
                                                      .setCancelAction(cancelVl, () -> {
                                                          user.getTimeMap().at(TimeKey.SCAFFOLD_TIMEOUT).update();
                                                          InventoryUtil.syncUpdateInventory(user.getPlayer());
                                                      })
                                                      .setDebug(() -> "Scaffold-Debug | Player: %s enforced delay: %f | real: %f | vl increase: %d".formatted(user.getPlayer().getName(), minExpectedAverage, actualAverage, vlIncrease)));
        }
    }

    private double calculateMinExpectedDelay(ScaffoldBatch.ScaffoldBlockPlace old, ScaffoldBatch.ScaffoldBlockPlace current, boolean moonwalk)
    {
        // Check if the player is building a straight line, or it follows a diagonal block pattern (which allows for faster placing.)
        // X            X
        // X  X  or  X  X
        //    X      X
        if (current.blockFace() != old.blockFace() && current.blockFace() != old.blockFace().getOppositeFace()) return diagonalDelay;

        // If the player is not sneaking, the delay is the normal delay.
        if (moonwalk || !current.sneaked() || !old.sneaked()) return normalDelay;

        // The player is sneaking.
        // The sneakingSlowAddition is applied for not building perfectly diagonal. This is not to be confused with the diagonalDelay in which the blocks do not form a straight line.
        return normalDelay + swiftSneakModifier(current.swiftSneakLevel()) * (sneakingAddition + (sneakingSlowAddition * Math.abs(Math.cos(2 * Math.toRadians(current.location().getYaw())))));
    }

    private static double swiftSneakModifier(int level)
    {
        if (level <= 0) return 1;
        else if (level < 5) return 1 - (0.2 * level);
        else return 0;
    }
}

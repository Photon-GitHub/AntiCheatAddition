package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.user.data.batch.ScaffoldBatch;
import de.photon.anticheataddition.util.datastructure.batch.BatchPreprocessors;
import de.photon.anticheataddition.util.datastructure.batch.SyncBatchProcessor;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import de.photon.anticheataddition.util.violationlevels.Flag;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

final class ScaffoldAverageBatchProcessor extends SyncBatchProcessor<ScaffoldBatch.ScaffoldBlockPlace>
{
    private static final Polynomial VL_CALCULATOR = new Polynomial(1.1, 5);

    /**
     * Delay when building diagonally, as this allows for faster placing than a straight line.
     * X            X
     * X  X  or  X  X
     * X      X
     */
    public static final double DIAGONAL_DELAY = 138;

    /**
     * Additional delay to account for the slower movement speed while sneaking.
     */
    public static final double SNEAKING_ADDITION = 90;

    /**
     * Additional delay as sneaking is fastest when facing the block in a 45° angle.
     */
    public static final double SNEAKING_SLOW_ADDITION = 110;

    private final int cancelVl = loadInt(".cancel_vl", 110);

    ScaffoldAverageBatchProcessor(ViolationModule module)
    {
        super(module, Set.of(ScaffoldBatch.SCAFFOLD_BATCH_EVENTBUS));
    }

    @Override
    public void processBatch(User user, List<ScaffoldBatch.ScaffoldBlockPlace> batch)
    {
        if (User.isUserInvalid(user, this.getModule())) return;

        final boolean moonwalk = batch.stream().filter(Predicate.not(ScaffoldBatch.ScaffoldBlockPlace::sneaked)).count() >= batch.size() / 2;
        final var delays = BatchPreprocessors.zipReduceToDoubleStatistics(batch,
                                                                          (old, cur) -> old.speedModifier() * cur.timeOffset(old),
                                                                          (old, cur) -> calculateMinExpectedDelay(old, cur, moonwalk));

        final double actualAverage = delays.get(0).getAverage();
        final double minExpectedAverage = delays.get(1).getAverage();

        Log.finer(() -> "Scaffold-Debug | Player: %s min delay: %f | real: %f".formatted(user.getPlayer().getName(), minExpectedAverage, actualAverage));

        // delta-times are not too low -> no flag
        if (actualAverage >= minExpectedAverage) return;

        final int vlIncrease = Math.min(130, VL_CALCULATOR.apply(minExpectedAverage - actualAverage).intValue());
        this.getModule().getManagement().flag(Flag.of(user)
                                                  .setAddedVl(vlIncrease)
                                                  .setCancelAction(cancelVl, () -> {
                                                      user.getTimeMap().at(TimeKey.SCAFFOLD_TIMEOUT).update();
                                                      InventoryUtil.syncUpdateInventory(user.getPlayer());
                                                  })
                                                  .setDebug(() -> "Scaffold-Debug | Player: %s enforced delay: %f | real: %f | vl increase: %d".formatted(user.getPlayer().getName(), minExpectedAverage, actualAverage, vlIncrease)));
    }

    private double calculateMinExpectedDelay(ScaffoldBatch.ScaffoldBlockPlace old, ScaffoldBatch.ScaffoldBlockPlace current, boolean moonwalk)
    {
        // Check if the player is building a straight line, or it follows a diagonal block pattern (which allows for faster placing.)
        // X            X
        // X  X  or  X  X
        //    X      X
        if (current.blockFace() != old.blockFace() && current.blockFace() != old.blockFace().getOppositeFace()) return DIAGONAL_DELAY;

        // If the player is not sneaking, the delay is the normal delay.
        if (moonwalk || !current.sneaked() || !old.sneaked()) return Scaffold.INSTANCE.getPlacementDelay();

        // The player is sneaking. The delay is the normal delay + the sneaking addition + the sneaking slow addition (which is dependent on the yaw).
        return Scaffold.INSTANCE.getPlacementDelay() + swiftSneakModifier(current.swiftSneakLevel()) * (SNEAKING_ADDITION + getDiagonalSneakDelay(current.location().getYaw()));
    }

    private static double getDiagonalSneakDelay(double yaw)
    {
        return SNEAKING_SLOW_ADDITION * Math.abs(Math.cos(2 * Math.toRadians(yaw)));
    }

    private static double swiftSneakModifier(int level)
    {
        return switch (level) {
            case 0 -> 1;
            case 1 -> 0.6;
            case 2 -> 0.25;
            // As you can just spam click while sneaking, scaffolding with swift_sneak is much faster than the actual speed increase suggests.
            default -> 0;
        };
    }
}

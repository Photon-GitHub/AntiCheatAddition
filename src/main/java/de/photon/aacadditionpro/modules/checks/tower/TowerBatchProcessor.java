package de.photon.aacadditionpro.modules.checks.tower;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.user.data.batch.TowerBatch;
import de.photon.aacadditionpro.util.datastructure.ImmutablePair;
import de.photon.aacadditionpro.util.datastructure.batch.AsyncBatchProcessor;
import de.photon.aacadditionpro.util.datastructure.batch.BatchPreprocessors;
import de.photon.aacadditionpro.util.datastructure.statistics.DoubleStatistics;
import de.photon.aacadditionpro.util.inventory.InventoryUtil;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.server.Movement;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import lombok.val;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class TowerBatchProcessor extends AsyncBatchProcessor<TowerBatch.TowerBlockPlace>
{
    /**
     * This {@link java.util.List} provides usually used and tested values to speed up performance and possibly low-
     * quality simulation results.
     */
    private static final List<Double> AMPLIFIER_CACHE = ImmutableList.of(
            // 478.4 * 0.925
            // No jump boost
            442.52D,
            // 578.4 * 0.925
            // Jump boost 1
            542.52D,
            // 290 * 0.925
            // Jump boost 2
            268.25,
            // 190 * 0.925
            // Jump boost 3
            175.75,
            // 140 * 0.925
            // Jump boost 4
            129.5);

    private final int cancelVl = AACAdditionPro.getInstance().getConfig().getInt(this.getModule().getConfigString() + ".cancel_vl");
    private final double towerLeniency = AACAdditionPro.getInstance().getConfig().getDouble(this.getModule().getConfigString() + ".tower_leniency");
    private final double levitationLeniency = AACAdditionPro.getInstance().getConfig().getDouble(this.getModule().getConfigString() + ".levitation_leniency");

    public TowerBatchProcessor(ViolationModule module)
    {
        super(module, ImmutableSet.of(TowerBatch.TOWER_BATCH_BROADCASTER));
    }

    @Override
    public void processBatch(User user, List<TowerBatch.TowerBlockPlace> batch)
    {
        val calcStatistics = new DoubleStatistics();
        val actualStatistics = new DoubleStatistics();
        val pairs = new ArrayList<>(BatchPreprocessors.zipOffsetOne(batch));

        for (ImmutablePair<TowerBatch.TowerBlockPlace, TowerBatch.TowerBlockPlace> pair : pairs) {
            calcStatistics.accept(calculateDelay(pair.getFirst()));
            actualStatistics.accept(pair.getFirst().timeOffset(pair.getSecond()));
        }

        val calcAvg = calcStatistics.getAverage();
        val actAvg = actualStatistics.getAverage();
        if (actAvg < calcAvg) {
            val vlToAdd = (int) Math.min(1 + Math.floor((calcAvg - actAvg) / 16), 100);
            this.getModule().getManagement().flag(Flag.of(user)
                                                      .setAddedVl(vlToAdd)
                                                      .setCancelAction(cancelVl, () -> {
                                                          user.getTimestampMap().at(TimestampKey.TOWER_TIMEOUT).update();
                                                          InventoryUtil.syncUpdateInventory(user.getPlayer());
                                                      })
                                                      .setEventNotCancelledAction(() -> DebugSender.getInstance().sendDebug("Tower-Verbose | Player: " + user.getPlayer().getName() + " expected time: " + calcAvg + " | real: " + actAvg)));
        }
    }

    /**
     * Calculates the time needed to place one block.
     */
    public double calculateDelay(TowerBatch.TowerBlockPlace blockPlace)
    {
        if (blockPlace.getLevitationLevel() != null) {
            // 0.9 Blocks per second per levitation level.
            return (900 / (blockPlace.getLevitationLevel() + 1D)) * towerLeniency * levitationLeniency;
        }

        // No JUMP_BOOST
        if (blockPlace.getJumpBoostLevel() == null) {
            return AMPLIFIER_CACHE.get(0);
        }

        // Player has JUMP_BOOST
        if (blockPlace.getJumpBoostLevel() < 0) {
            // Negative JUMP_BOOST -> Not allowed to place blocks -> Very high delay
            return 1500;
        }

        if (blockPlace.getJumpBoostLevel() + 1 < AMPLIFIER_CACHE.size()) {
            return AMPLIFIER_CACHE.get(blockPlace.getJumpBoostLevel() + 1);
        }

        // The velocity in the beginning
        Vector currentVelocity = new Vector(0, Movement.PLAYER.getJumpYMotion(blockPlace.getJumpBoostLevel()), 0);

        // The first tick (1) happens here
        double currentBlockValue = currentVelocity.getY();
        Double landingBlock = null;

        // Start the tick-loop at 2 due to the one tick outside.
        for (short ticks = 2; ticks < 160; ++ticks) {
            currentVelocity = Movement.PLAYER.applyGravitationAndAirResistance(currentVelocity);

            // Break as the player has already reached the max height (no more blocks to place below).
            if (currentVelocity.getY() <= 0) {
                if (landingBlock == null) {
                    landingBlock = Math.floor(currentBlockValue);
                } else if (currentBlockValue <= landingBlock) {
                    // If the result is lower here, the detection is more lenient.
                    // * 50 : Convert ticks to milliseconds
                    // 0.92 is the required simulation leniency I got from testing.
                    // 0.925 is additional leniency
                    // -15 is special leniency for high jump boost environments.
                    return ((((ticks * 50) / landingBlock) * 0.92 * 0.925) - 15) * towerLeniency;
                }
            }

            currentBlockValue += currentVelocity.getY();
        }

        // Too high movement; no checking
        return 0;
    }
}

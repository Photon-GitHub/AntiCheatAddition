package de.photon.aacadditionpro.modules.checks.tower;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.batch.TowerBatch;
import de.photon.aacadditionpro.util.datastructure.batch.AsyncBatchProcessor;
import de.photon.aacadditionpro.util.server.Movement;
import lombok.val;
import org.bukkit.util.Vector;

import java.util.List;

public class TowerBatchProcessor extends AsyncBatchProcessor<TowerBatch.TowerBlockPlace>
{
    private static final double TOWER_LENIENCY = AACAdditionPro.getInstance().getConfig().getDouble("Tower.tower_leniency");
    private static final double LEVITATION_LENIENCY = AACAdditionPro.getInstance().getConfig().getDouble("Tower.levitation_leniency");

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

    private TowerBatchProcessor()
    {
        super(ImmutableSet.of(TowerBatch.TOWER_BATCH_BROADCASTER));
    }

    @Override
    public void processBatch(User user, List<TowerBatch.TowerBlockPlace> batch)
    {
        final double[] results = new double[2];


        val actualTime = batch.stream().
                IterationUtil.twoObjectsIterationToEnd(batch, (old, current) -> {
            // [0] = Expected time; [1] = Real time
            results[0] += current.calculateDelay();
            results[1] += (current.getTime() - old.getTime());
        });

        // Average
        results[0] /= batch.size();
        results[1] /= batch.size();

        if (results[1] < results[0]) {
            final int vlToAdd = (int) Math.min(1 + Math.floor((results[0] - results[1]) / 16), 100);

            // Violation-Level handling
            Tower.getInstance().getViolationLevelManagement().flag(user.getPlayer(), vlToAdd, Tower.getInstance().getCancelVl(), () ->
            {
                user.getTimestampMap().updateTimeStamp(TimestampKey.TOWER_TIMEOUT);
                InventoryUtils.syncUpdateInventory(user.getPlayer());
                // If not cancelled run the verbose message with additional data
            }, () -> VerboseSender.getInstance().sendVerboseMessage("Tower-Verbose | Player: " + user.getPlayer().getName() + " expected time: " + results[0] + " | real: " + results[1]));
        }
    }

    /**
     * Calculates the time needed to place one block.
     */
    public double calculateDelay(TowerBatch.TowerBlockPlace blockPlace)
    {
        if (blockPlace.getLevitationLevel() != null) {
            // 0.9 Blocks per second per levitation level.
            return (900 / (blockPlace.getLevitationLevel() + 1D)) * TOWER_LENIENCY * LEVITATION_LENIENCY;
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
                    return ((((ticks * 50) / landingBlock) * 0.92 * 0.925) - 15) * TOWER_LENIENCY;
                }
            }

            currentBlockValue += currentVelocity.getY();
        }

        // Too high movement; no checking
        return 0;
    }
}

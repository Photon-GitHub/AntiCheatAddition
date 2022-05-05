package de.photon.anticheataddition.modules.checks.tower;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.user.data.batch.TowerBatch;
import de.photon.anticheataddition.util.datastructure.batch.AsyncBatchProcessor;
import de.photon.anticheataddition.util.datastructure.batch.BatchPreprocessors;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import de.photon.anticheataddition.util.mathematics.TimeUtil;
import de.photon.anticheataddition.util.minecraft.movement.Movement;
import de.photon.anticheataddition.util.minecraft.movement.MovementSimulator;
import de.photon.anticheataddition.util.violationlevels.Flag;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

public final class TowerBatchProcessor extends AsyncBatchProcessor<TowerBatch.TowerBlockPlace>
{
    private static final Polynomial VL_CALCULATOR = new Polynomial(0.37125, 1);

    /**
     * This {@link java.util.List} provides usually used and tested values to speed up performance and possibly low-
     * quality simulation results.
     */
    private static final List<Double> FIRST_DELAYS = List.of(
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

    private final int cancelVl = loadInt(".cancel_vl", 6);
    private final double towerLeniency = loadDouble(".tower_leniency", 1);
    private final double levitationLeniency = loadDouble(".levitation_leniency", 0.95);

    public TowerBatchProcessor(ViolationModule module)
    {
        super(module, Set.of(TowerBatch.TOWER_BATCH_EVENTBUS));
    }

    @Override
    public void processBatch(User user, List<TowerBatch.TowerBlockPlace> batch)
    {
        val statistics = BatchPreprocessors.zipReduceToDoubleStatistics(batch,
                                                                        (old, cur) -> calculateDelay(old),
                                                                        TowerBatch.TowerBlockPlace::timeOffset);

        val calcAvg = statistics.get(0).getAverage();
        val actAvg = statistics.get(1).getAverage();

        if (actAvg < calcAvg) {
            val vlToAdd = Math.min(VL_CALCULATOR.apply(calcAvg - actAvg).intValue(), 1000);
            this.getModule().getManagement().flag(Flag.of(user)
                                                      .setAddedVl(vlToAdd)
                                                      .setCancelAction(cancelVl, () -> {
                                                          user.getTimeMap().at(TimeKey.TOWER_TIMEOUT).update();
                                                          InventoryUtil.syncUpdateInventory(user.getPlayer());
                                                      })
                                                      .setDebug(() -> "Tower-Debug | Player: " + user.getPlayer().getName() + " expected time: " + calcAvg + " | real: " + actAvg));
        }
    }

    /**
     * Calculates the time needed to place one block.
     */
    public double calculateDelay(TowerBatch.TowerBlockPlace blockPlace)
    {
        // Levitation handling.
        if (blockPlace.getLevitation().isPresent()) {
            // 0.9 Blocks per second per levitation level.
            return (900 / (blockPlace.getLevitation().get().getAmplifier() + 1D)) * towerLeniency * levitationLeniency;
        }

        // No Jump Boost
        if (blockPlace.getJumpBoost().isEmpty()) return FIRST_DELAYS.get(0);

        val jumpBoost = blockPlace.getJumpBoost().get().getAmplifier();
        // Negative Jump Boost -> Player is not allowed to place blocks -> Very high delay
        if (jumpBoost < 0) return 1500;

        // Normal Jump Boost in cache
        if (jumpBoost + 1 < FIRST_DELAYS.size()) return FIRST_DELAYS.get(jumpBoost + 1);

        // Start the simulation.
        val startLocation = new Location(null, 0, 0, 0);
        val currentVelocity = new Vector(0, Movement.PLAYER.getJumpYMotion(jumpBoost), 0);

        val simulator = new MovementSimulator(startLocation, currentVelocity, Movement.PLAYER);
        simulator.tick();
        simulator.tickUntil(sim -> sim.getVelocity().getY() <= 0, 200);
        final double landingBlockY = simulator.getCurrent().getBlock().getY();
        simulator.tickUntil(sim -> sim.getCurrent().getY() <= landingBlockY, 50);

        // If the result is lower here, the detection is more lenient.
        // * 50 : Convert ticks to milliseconds
        // 0.92 is the required simulation leniency I got from testing.
        // 0.925 is additional leniency
        // -15 is special leniency for high jump boost environments.
        return (((TimeUtil.toMillis(simulator.getTick()) / landingBlockY) * 0.92D * 0.925D) - 15D) * towerLeniency;
    }
}

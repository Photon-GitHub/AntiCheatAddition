package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.subdata.InventoryData;
import de.photon.aacadditionpro.user.subdata.datawrappers.InventoryClick;
import de.photon.aacadditionpro.util.datastructures.batch.AsyncBatchProcessor;
import de.photon.aacadditionpro.util.datastructures.iteration.IterationUtil;
import de.photon.aacadditionpro.util.mathematics.MathUtils;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import lombok.Getter;

import java.util.List;

public class AverageHeuristicBatchProcessor extends AsyncBatchProcessor<InventoryClick>
{
    @Getter
    private static final AverageHeuristicBatchProcessor instance = new AverageHeuristicBatchProcessor();

    private AverageHeuristicBatchProcessor()
    {
        super(InventoryData.AVERAGE_HEURISTICS_BATCH_SIZE);
    }

    @Override
    public void processBatch(User user, List<InventoryClick> batch)
    {
        final List<InventoryClick.BetweenClickInformation> betweenClicks = IterationUtil.pairCombine(batch, (old, current) -> old.inventory.equals(current.inventory), InventoryClick.BetweenClickInformation::new);

        // Not enough data to check as the player opened many different inventories.
        if (betweenClicks.size() < 8) {
            user.getInventoryData().averageHeuristicMisclicks = 0;
            return;
        }

        final double average = betweenClicks.stream().mapToDouble(between -> between.timeDelta).average().orElseThrow(() -> new IllegalArgumentException("Could not get average of BetweenClick stream."));
        final double squaredErrorsSum = betweenClicks.stream().mapToDouble(between -> MathUtils.squaredError(average, between.timeDelta)).sum();

        // One time 2 ticks offset and 2 times 1 tick offset * 15 minimum vl = 168750
        // 2500 error sum is legit achievable.
        // +1 to avoid division by 0
        double vl = 40000 / (squaredErrorsSum + 1);

        // Average below 1 tick is considered inhuman and increases vl.
        final double ticks = average / 50;
        final double averageMultiplier = 1.65 + ticks * (-0.171127 + (0.00709709 - 0.000102881 * ticks) * ticks);
        vl *= Math.max(averageMultiplier, 0.5);

        // Make sure that misclicks are applied correctly.
        vl /= (user.getInventoryData().averageHeuristicMisclicks + 1);

        // Mitigation for possibly better players.
        vl -= 10;

        // Too low vl.
        if (vl < 10) {
            return;
        }

        final double finalVl = vl;
        Inventory.getInstance().getViolationLevelManagement().flag(user.getPlayer(),
                                                                   (int) Math.min(vl, 35),
                                                                   0,
                                                                   () -> {},
                                                                   () -> VerboseSender.getInstance().sendVerboseMessage("Inventory-Verbose | Player: " + user.getPlayer().getName() + " has bot-like click delays. (SE: " + squaredErrorsSum + " | A: " + average + " | MC: " + user.getInventoryData().averageHeuristicMisclicks + " | VLU: " + finalVl + ")"));
        user.getInventoryData().averageHeuristicMisclicks = 0;
    }
}
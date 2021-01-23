package de.photon.aacadditionproold.modules.checks.tower;

import de.photon.aacadditionproold.user.TimestampKey;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.subdata.TowerData;
import de.photon.aacadditionproold.user.subdata.datawrappers.TowerBlockPlace;
import de.photon.aacadditionproold.util.datastructures.batch.AsyncBatchProcessor;
import de.photon.aacadditionproold.util.datastructures.iteration.IterationUtil;
import de.photon.aacadditionproold.util.inventory.InventoryUtils;
import de.photon.aacadditionproold.util.messaging.VerboseSender;
import lombok.Getter;

import java.util.List;

public class TowerBatchProcessor extends AsyncBatchProcessor<TowerBlockPlace>
{
    @Getter
    private static final TowerBatchProcessor instance = new TowerBatchProcessor();

    private TowerBatchProcessor()
    {
        super(TowerData.TOWER_BATCH_SIZE);
    }

    @Override
    public void processBatch(User user, List<TowerBlockPlace> batch)
    {
        final double[] results = new double[2];

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
}

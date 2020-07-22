package de.photon.aacadditionpro.modules.checks.tower;

import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.subdata.TowerData;
import de.photon.aacadditionpro.user.subdata.datawrappers.TowerBlockPlace;
import de.photon.aacadditionpro.util.VerboseSender;
import de.photon.aacadditionpro.util.datastructures.batch.BatchProcessor;
import de.photon.aacadditionpro.util.datastructures.stream.IterationUtil;
import de.photon.aacadditionpro.util.inventory.InventoryUtils;

import java.util.List;

public class TowerBatchProcessor extends BatchProcessor<TowerBlockPlace>
{
    public TowerBatchProcessor()
    {
        super(TowerData.TOWER_BATCH_SIZE);
    }

    @Override
    public void processBatch(User user, List<TowerBlockPlace> batch)
    {
        final double[] results = new double[2];
        IterationUtil.twoObjectsIterationToEnd(batch, (old, current) -> {
            // [0] = Expected time; [1] = Real time
            results[0] += current.getDelay();
            results[1] += (old.getTime() - current.getTime());
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

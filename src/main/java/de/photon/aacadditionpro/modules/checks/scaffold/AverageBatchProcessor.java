package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.subdata.ScaffoldData;
import de.photon.aacadditionpro.user.subdata.datawrappers.ScaffoldBlockPlace;
import de.photon.aacadditionpro.util.datastructures.batch.AsyncBatchProcessor;
import de.photon.aacadditionpro.util.datastructures.iteration.IterationUtil;
import de.photon.aacadditionpro.util.inventory.InventoryUtils;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import lombok.Getter;

import java.util.List;

public class AverageBatchProcessor extends AsyncBatchProcessor<ScaffoldBlockPlace>
{
    @Getter
    private static final AverageBatchProcessor instance = new AverageBatchProcessor();

    private AverageBatchProcessor()
    {
        super(ScaffoldData.BATCH_SIZE);
    }

    @Override
    public void processBatch(User user, List<ScaffoldBlockPlace> batch)
    {
        /*
         * Used to calculate the average and expected time span between the ScaffoldBlockPlaces in the buffer.
         * Also clears the buffer.
         *
         * @return an array with the following contents:<br>
         * [0] = Expected time <br>
         * [1] = Real time <br>
         */
        final double[] result = new double[2];

        // -1 because there is one pop to fill the "last" variable in the beginning.
        final int divisor = batch.size() - 1;

        final boolean moonwalk = batch.stream().filter(blockPlace -> !blockPlace.isSneaked()).count() >= ScaffoldData.BATCH_SIZE / 2;

        IterationUtil.twoObjectsIterationToEnd(batch, (old, current) -> {
            double delay;
            if (current.getBlockFace() == old.getBlockFace() || current.getBlockFace() == old.getBlockFace().getOppositeFace()) {
                delay = ScaffoldData.DELAY_NORMAL;

                if (!moonwalk && current.isSneaked() && old.isSneaked()) {
                    delay += ScaffoldData.SNEAKING_ADDITION + (ScaffoldData.SNEAKING_SLOW_ADDITION * Math.abs(Math.cos(2D * current.getYaw())));
                }
            } else {
                delay = ScaffoldData.DELAY_DIAGONAL;
            }

            result[0] += delay;

            // last - current to calculate the delta as the more recent time is always in last.
            result[1] += (current.getTime() - old.getTime()) * old.getSpeedModifier();
        });

        result[0] /= divisor;
        result[1] /= divisor;

        // delta-times are too low -> flag
        if (result[1] < result[0]) {
            // Calculate the vl
            final int vlIncrease = (int) (4 * Math.min(Math.ceil((result[0] - result[1]) / 15D), 6));
            Scaffold.getInstance().getViolationLevelManagement().flag(user.getPlayer(), vlIncrease, Scaffold.getInstance().getCancelVl(), () -> {
                user.getTimestampMap().updateTimeStamp(TimestampKey.SCAFFOLD_TIMEOUT);
                InventoryUtils.syncUpdateInventory(user.getPlayer());
            }, () -> VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " enforced delay: " + result[0] + " | real: " + result[1] + " | vl increase: " + vlIncrease));
        }
    }
}

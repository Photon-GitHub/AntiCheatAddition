package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.datawrappers.ScaffoldBlockPlace;
import de.photon.AACAdditionPro.util.datastructures.buffer.ConditionalCleanBuffer;
import de.photon.AACAdditionPro.util.datastructures.buffer.DequeBuffer;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import lombok.Getter;

public class ScaffoldData extends TimeData
{
    // Default buffer size is 6, being well tested.
    private static final int BUFFER_SIZE = 6;
    // Use static here as ScaffoldDatas are often created.
    private static final double DELAY_NORMAL = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.SCAFFOLD.getConfigString() + ".parts.average.delays.normal");
    private static final double SNEAKING_ADDITION = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.SCAFFOLD.getConfigString() + ".parts.average.delays.sneaking_addition");
    private static final double SNEAKING_SLOW_ADDITION = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.SCAFFOLD.getConfigString() + ".parts.average.delays.sneaking_slow_addition");
    private static final double DELAY_DIAGONAL = AACAdditionPro.getInstance().getConfig().getInt(ModuleType.SCAFFOLD.getConfigString() + ".parts.average.delays.diagonal");

    /**
     * This is used to determine wrong angles while scaffolding.
     * One wrong angle might be legit, but more instances are a clear hint.
     */
    public long angleFails = 0;

    /**
     * This is used to determine fast rotations prior to scaffolding.
     * One fast rotation might be legit, but more instances are a clear hint.
     */
    public long rotationFails = 0;

    /**
     * This is used to suspicious locations.
     * One fast rotation might be legit, but more instances are a clear hint.
     */
    public long safewalkTypeOneFails = 0;

    /**
     * This is used to suspicious locations.
     * One fast rotation might be legit, but more instances are a clear hint.
     */
    public long safewalkTypeTwoFails = 0;

    /**
     * This is used to determine sprinting during scaffolding.
     * Some sprinting might be legit.
     */
    public long sprintingFails = 0;

    // Add a dummy block to start with in order to make sure that the queue is never empty.
    @Getter
    private final DequeBuffer<ScaffoldBlockPlace> scaffoldBlockPlaces;

    public ScaffoldData(final User user)
    {
        super(user, 0);

        scaffoldBlockPlaces = new ConditionalCleanBuffer<ScaffoldBlockPlace>(BUFFER_SIZE)
        {
            @Override
            protected boolean verifyObject(ScaffoldBlockPlace object)
            {
                final ScaffoldBlockPlace last = this.getDeque().peek();
                return last == null || BlockUtils.isNext(last.getBlock(), object.getBlock(), true);
            }
        };
    }

    /**
     * Used to calculate the average and expected time span between the {@link ScaffoldBlockPlace}s in the buffer.
     * Also clears the buffer.
     *
     * @return an array with the following contents:<br>
     * [0] = Expected time <br>
     * [1] = Real time <br>
     */
    public double[] calculateTimes()
    {
        final double[] result = new double[2];

        // -1 because there is one pop to fill the "last" variable in the beginning.
        final int divisor = this.scaffoldBlockPlaces.getDeque().size() - 1;

        final boolean moonwalk = this.scaffoldBlockPlaces.getDeque().stream().filter(blockPlace -> !blockPlace.isSneaked()).count() >= BUFFER_SIZE / 2;

        this.scaffoldBlockPlaces.clearLastTwoObjectsIteration(
                (last, current) ->
                {
                    double delay;
                    if (last.getBlockFace() == current.getBlockFace() || last.getBlockFace() == current.getBlockFace().getOppositeFace()) {
                        delay = DELAY_NORMAL;

                        if (!moonwalk && last.isSneaked() && current.isSneaked()) {
                            delay += SNEAKING_ADDITION + (SNEAKING_SLOW_ADDITION * Math.abs(Math.cos(2 * current.getYaw())));
                        }
                    } else {
                        delay = DELAY_DIAGONAL;
                    }

                    result[0] += delay;

                    // last - current to calculate the delta as the more recent time is always in last.
                    result[1] += (last.getTime() - current.getTime()) * current.getSpeedModifier();
                });

        result[0] /= divisor;
        result[1] /= divisor;
        return result;
    }
}

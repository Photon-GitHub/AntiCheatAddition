package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.datastructures.ConditionalBuffer;
import de.photon.AACAdditionPro.util.datawrappers.BlockPlace;
import de.photon.AACAdditionPro.util.datawrappers.TowerBlockPlace;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import lombok.Getter;

/**
 * Used to store {@link BlockPlace}s. The {@link TimeData} is used for timeouts.
 */
public class TowerData extends TimeData
{
    // Default buffer size is 6, being well tested.
    private static int BUFFER_SIZE = 6;

    @Getter
    protected final ConditionalBuffer<TowerBlockPlace> blockPlaces = new ConditionalBuffer<TowerBlockPlace>(BUFFER_SIZE)
    {
        @Override
        protected boolean verifyObject(TowerBlockPlace object)
        {
            return blockPlaces.isEmpty() ||
                   BlockUtils.isNext(blockPlaces.peek().getBlock(), object.getBlock(), false);
        }
    };

    public TowerData(final User user)
    {
        super(user, 0);
    }

    /**
     * Adds a {@link TowerBlockPlace} to the buffer
     *
     * @param towerBlockPlace The {@link TowerBlockPlace} which should be added.
     *
     * @return whether or not the {@link TowerBlockPlace} was accepted by {@link ConditionalBuffer#verifyObject(Object)}
     */
    public boolean bufferBlockPlace(final TowerBlockPlace towerBlockPlace)
    {
        return blockPlaces.bufferObject(towerBlockPlace);
    }

    /**
     * Used to calculate the average time span between the {@link TowerBlockPlace}s in the buffer
     * Also clears the buffer.
     *
     * @return the average time between {@link TowerBlockPlace}s.
     */
    public double calculateAverageTime()
    {
        // fraction[0] is the enumerator
        // fraction[1] is the divider
        final double[] fraction = new double[2];
        // -1 because there is one pop to fill the "last" variable in the beginning.
        fraction[1] = this.blockPlaces.size() - 1;
        this.blockPlaces.clearLastTwoObjectsIteration((last, current) -> fraction[0] += (last.getTime() - current.getTime()));
        return fraction[0] / fraction[1];
    }
}
package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.util.storage.datawrappers.BlockPlace;
import de.photon.AACAdditionPro.util.storage.management.ConditionalBuffer;
import de.photon.AACAdditionPro.util.world.BlockUtils;


/**
 * Used to store {@link BlockPlace}s. The {@link TimeData} is used for timeouts.
 */
public class BlockPlaceData extends TimeData
{
    protected final ConditionalBuffer<BlockPlace> blockPlaces;

    public BlockPlaceData(final boolean horizontal, final int bufferSize, final User theUser)
    {
        super(false, theUser);

        blockPlaces = new ConditionalBuffer<BlockPlace>(bufferSize)
        {
            @Override
            public boolean verifyObject(final BlockPlace object)
            {
                return blockPlaces.isEmpty() || BlockUtils.isNext(blockPlaces.get(blockPlaces.size() - 1).getBlock(), object.getBlock(), horizontal);
            }
        };
    }

    /**
     * Adds a {@link BlockPlace} to the buffer
     *
     * @param blockplace The blockplace which should be added.
     *
     * @return true if the buffersize is bigger than the max_size.
     */
    public boolean bufferBlockPlace(final BlockPlace blockplace)
    {
        return blockPlaces.bufferObject(blockplace);
    }

    /**
     * The public method to calculate the time.
     * This method is designed to be overridden.
     *
     * @return {@link BlockPlaceData}{@link #calculateAverageTime()} if not overridden.
     */
    public double calculateRealTime()
    {
        return this.calculateAverageTime();
    }

    /**
     * Used to calculate the average time span between the {@link BlockPlace}s in the buffer
     * Also clears the buffer.
     *
     * @return the average time between {@link BlockPlace}s.
     */
    private double calculateAverageTime()
    {
        // fraction[0] is the enumerator
        // fraction[1] is the divider
        final double[] fraction = new double[2];

        this.blockPlaces.clearLastTwoObjectsIteration(
                (last, current) ->
                {
                    fraction[0] += (last.getTime() - current.getTime());
                    fraction[1]++;
                });
        return fraction[0] / fraction[1];
    }

    /**
     * @return the internal {@link ConditionalBuffer}.
     */
    public ConditionalBuffer<BlockPlace> getBlockPlaces()
    {
        return blockPlaces;
    }
}
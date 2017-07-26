package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.util.storage.datawrappers.BlockPlace;
import de.photon.AACAdditionPro.util.storage.management.Buffer;
import de.photon.AACAdditionPro.util.world.BlockUtils;


/**
 * Used to store {@link BlockPlace}s. The {@link TimeData} is used for timeouts.
 */
public abstract class BlockPlaceData extends TimeData
{
    protected final Buffer<BlockPlace> blockPlaces;
    private final int buffer_size;

    protected BlockPlaceData(final boolean horizontal, final int buffer_size, final User theUser)
    {
        super(false, theUser);
        this.buffer_size = buffer_size;

        blockPlaces = new Buffer<BlockPlace>(buffer_size)
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
     * @return true if the buffersize is bigger than the max_size.
     */
    public boolean bufferBlockPlace(final BlockPlace blockplace)
    {
        return blockPlaces.bufferObject(blockplace);
    }

    public abstract double calculateRealTime();

    /**
     * Used to calculate the average time span between the {@link BlockPlace}s in the buffer
     * Also clears the buffer.
     *
     * @return the average time between {@link BlockPlace}s.
     */
    protected double calculateAverageTime()
    {
        // fraction[0] is the enumerator
        // fraction[1] is the divider
        final double[] fraction = new double[2];

        this.blockPlaces.clearLastObjectIteration(
                (last, current) ->
                {
                    fraction[0] += last.getTime() - current.getTime();
                    fraction[1]++;
                });
        return fraction[0] / fraction[1];
    }

    public Buffer<BlockPlace> getBlockPlaces()
    {
        return blockPlaces;
    }

    public int getBuffer_size()
    {
        return buffer_size;
    }

}
package de.photon.aacadditionpro.util.datastructure.buffer;

public interface FixedSizeBuffer<T> extends Buffer<T>
{
    /**
     * @return the maximum size the {@link FixedSizeBuffer} can grow to.
     */
    int getMaxSize();

    /**
     * This not only clears the {@link FixedSizeBuffer} but also makes sure that all elements in this
     * {@link FixedSizeBuffer} will be overwritten with <code>null</code> values.
     * <p></p>
     * The {@link #clear()} method might only set the {@link FixedSizeBuffer} up to overwrite its current elements in the future.
     */
    void fullClear();
}

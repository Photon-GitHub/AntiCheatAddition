package de.photon.AACAdditionPro.util.datastructures.buffer;

/**
 * Describes a buffer that has a maximum length and discards the least recently added element once this limit is
 * surpassed.
 */
public interface ContinuousBuffer<T> extends Buffer<T>
{
    /**
     * Represents the maximum size of the {@link ContinuousBuffer}.
     */
    int getMaxSize();

    /**
     * Defines a code that will be run when an element is forgotten.
     */
    default void onForget(T forgotten) {}

    /**
     * This not only clears the {@link ContinuousBuffer} but also makes sure that all elements in this
     * {@link ContinuousBuffer} will be overwritten with <code>null</code> values.
     * <p>
     * {@link #clear()} will only set the {@link ContinuousBuffer} up to overwrite the old values, but doesn't actually
     * clear them.
     */
    void forceClear();
}

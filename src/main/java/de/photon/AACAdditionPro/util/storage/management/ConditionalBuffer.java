package de.photon.AACAdditionPro.util.storage.management;

public abstract class ConditionalBuffer<T> extends Buffer<T>
{
    protected ConditionalBuffer(final int bufferSize)
    {
        super(bufferSize);
    }

    /**
     * This is used to verify an object before it gets added to the buffer,
     * and therefore useful for checking e.g. adjacency of blocks or similar.
     *
     * @return true if the object should be added to the buffer and false if the buffer should be cleared
     */
    protected abstract boolean verifyObject(T object);

    /**
     * Adds a {@link T} to the buffer, or clears the buffer if verifyObject returns false
     *
     * @param object The object which should be added.
     *
     * @return true if the buffersize is bigger than the max_size.
     */
    @Override
    public boolean bufferObject(final T object)
    {
        if (verifyObject(object))
        {
            this.add(object);
        }
        else
        {
            this.clear();
        }
        return this.size() >= this.getBufferSize();
    }
}

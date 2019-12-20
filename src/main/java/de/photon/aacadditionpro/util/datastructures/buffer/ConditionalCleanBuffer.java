package de.photon.aacadditionpro.util.datastructures.buffer;

/**
 * This describes a buffer that is cleaned when a new element does not fulfill certain conditions.
 */
public abstract class ConditionalCleanBuffer<T> extends SimpleBuffer<T>
{
    public ConditionalCleanBuffer(int capacity)
    {
        super(capacity);
    }

    /**
     * This is used to verify an object before it gets added to the buffer,
     * and therefore useful for checking e.g. adjacency of blocks or similar.
     *
     * @return true if the object should be added to the buffer and false if the buffer should be cleared
     */
    protected abstract boolean verifyObject(T object);

    @Override
    public boolean bufferObject(T object)
    {
        if (!this.verifyObject(object)) {
            this.getDeque().clear();
        }
        return super.bufferObject(object);
    }
}

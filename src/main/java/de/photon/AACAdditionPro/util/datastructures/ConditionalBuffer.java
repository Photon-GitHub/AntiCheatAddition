package de.photon.AACAdditionPro.util.datastructures;

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

    @Override
    public boolean bufferObjectIgnoreSize(T object)
    {
        if (verifyObject(object))
        {
            this.push(object);
            return true;
        }
        else
        {
            this.clear();
            return false;
        }
    }
}

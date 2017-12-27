package de.photon.AACAdditionPro.util.storage.management;

import java.util.ArrayList;
import java.util.function.BiConsumer;

public class Buffer<T> extends ArrayList<T>
{
    protected final int buffer_size;

    public Buffer(final int buffer_size)
    {
        super(buffer_size);
        this.buffer_size = buffer_size;
    }

    /**
     * Adds a {@link T} to the buffer, or clears the buffer if verifyObject returns false
     *
     * @param object The object which should be added.
     *
     * @return true if the buffersize is bigger than the max_size.
     */
    public boolean bufferObject(final T object)
    {
        this.add(object);
        return this.size() >= this.buffer_size;
    }

    /**
     * Iterates through the buffer and clears it at the same time
     * The most recently added element will always be in last.
     *
     * @param code the code which should be run in each cycle
     */
    public void clearLastObjectIteration(final BiConsumer<T, T> code)
    {
        if (!this.isEmpty())
        {
            T last = this.remove(this.size() - 1);
            T current;
            while (!this.isEmpty())
            {
                current = this.remove(this.size() - 1);
                code.accept(last, current);
                last = current;
            }
        }
    }
}

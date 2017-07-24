package de.photon.AACAdditionPro.util.storage.management;

import java.util.ArrayList;
import java.util.Stack;
import java.util.function.BiConsumer;

public abstract class Buffer<T> extends ArrayList<T>
{
    private final int buffer_size;

    protected Buffer(final int buffer_size)
    {
        super(buffer_size);
        this.buffer_size = buffer_size;
    }

    /**
     * This is used to verify an object before it gets added to the buffer,
     * and therefore useful for checking e.g. adjacency of blocks or similar.
     *
     * @return true if the object should be added to the buffer and false if the buffer should be cleared
     */
    public abstract boolean verifyObject(T object);

    /**
     * Adds a {@link T} to the buffer, or clears the buffer if verifyObject returns false
     *
     * @param object The object which should be added.
     *
     * @return true if the buffersize is bigger than the max_size.
     */
    public boolean bufferObject(final T object)
    {
        if (verifyObject(object)) {
            this.add(object);
        } else {
            this.clear();
        }
        return this.size() >= this.buffer_size;
    }

    /**
     * Iterates through the buffer and clears it at the same time
     * During the first cycle the first object of the {@link Stack} will be in the last - variable,
     * the second one in the current - variable
     *
     * @param code the code which should be run in each cycle
     */
    public void clearLastObjectIteration(final BiConsumer<T, T> code)
    {
        if (!this.isEmpty()) {
            T last = this.remove(this.size() - 1);
            T current;
            while (!this.isEmpty()) {
                current = this.remove(this.size() - 1);
                code.accept(last, current);
                last = current;
            }
        }
    }
}

package de.photon.AACAdditionPro.util.storage.management;

import lombok.Getter;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Buffer<T> extends ArrayList<T>
{
    @Getter
    private final int bufferSize;

    public Buffer(final int bufferSize)
    {
        super(bufferSize);
        this.bufferSize = bufferSize;
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
        return this.size() >= this.bufferSize;
    }

    /**
     * Iterates through the buffer and clears it at the same time.
     *
     * @param lastObjectConsumer the code which should run for each element.
     */
    public void clearLastObjectIteration(final Consumer<T> lastObjectConsumer)
    {
        while (!this.isEmpty())
        {
            T current = this.remove(this.size() - 1);
            lastObjectConsumer.accept(current);
        }
    }

    /**
     * Iterates through the buffer and clears it at the same time
     * The most recently added element will always be in last.
     * After one cycle the current object will be written to last.
     *
     * @param lastObjectsConsumer the code which should be run in each cycle, consuming the last and the current object.
     */
    public void clearLastTwoObjectsIteration(final BiConsumer<T, T> lastObjectsConsumer)
    {
        if (!this.isEmpty())
        {
            T last = this.remove(this.size() - 1);
            T current;
            while (!this.isEmpty())
            {
                current = this.remove(this.size() - 1);
                lastObjectsConsumer.accept(last, current);
                last = current;
            }
        }
    }
}

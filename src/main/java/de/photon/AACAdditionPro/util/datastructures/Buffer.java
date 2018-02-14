package de.photon.AACAdditionPro.util.datastructures;

import lombok.Getter;

import java.util.Stack;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Buffer<T> extends Stack<T>
{
    @Getter
    private final int bufferSize;

    public Buffer(final int bufferSize)
    {
        this.bufferSize = bufferSize;
    }

    /**
     * Adds an {@link Object} of type {@link T} to the buffer
     *
     * @param object The object which should be added.
     *
     * @return true if the buffersize is bigger than the max_size.
     */
    public boolean bufferObject(final T object)
    {
        this.bufferObjectIgnoreSize(object);
        return this.size() >= this.bufferSize;
    }

    /**
     * Adds an {@link Object} of type {@link T} to the buffer
     *
     * @param object The object which should be added.
     *
     * @return true if the {@link Object} was accepted
     */
    public boolean bufferObjectIgnoreSize(final T object)
    {
        this.push(object);
        return true;
    }

    public boolean hasReachedBufferSize()
    {
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
            lastObjectConsumer.accept(this.pop());
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
            T last = this.pop();
            T current;
            while (!this.isEmpty())
            {
                current = this.pop();
                lastObjectsConsumer.accept(last, current);
                last = current;
            }
        }
    }
}

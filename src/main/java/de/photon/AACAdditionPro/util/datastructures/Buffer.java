package de.photon.AACAdditionPro.util.datastructures;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Buffer<T>
{
    private final int capacity;
    @Getter
    private Deque<T> deque;

    public Buffer(int capacity)
    {
        this.capacity = capacity;
        this.deque = new ArrayDeque<>(this.capacity);
    }

    /**
     * Adds an {@link Object} of type {@link T} to the buffer
     *
     * @param object The object which should be added.
     *
     * @return true if the size of the buffer is bigger than the max_size.
     */
    public boolean bufferObject(final T object)
    {
        this.deque.push(object);
        return this.hasReachedBufferSize();
    }

    public boolean hasReachedBufferSize()
    {
        return this.deque.size() >= this.capacity;
    }

    /**
     * Iterates through the buffer and clears it at the same time.
     *
     * @param lastObjectConsumer the code which should run for each element.
     */
    public void clearLastObjectIteration(final Consumer<T> lastObjectConsumer)
    {
        while (!this.deque.isEmpty())
        {
            lastObjectConsumer.accept(this.deque.pop());
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
        if (!this.deque.isEmpty())
        {
            T last = this.deque.pop();
            T current;
            while (!this.deque.isEmpty())
            {
                current = this.deque.pop();
                lastObjectsConsumer.accept(last, current);
                last = current;
            }
        }
    }
}

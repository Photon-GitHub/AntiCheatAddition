package de.photon.aacadditionpro.util.datastructures.buffer;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;

/**
 * This uses an array to implement a {@link ContinuousBuffer}
 */
public class ContinuousArrayBuffer<T> implements ContinuousBuffer<T>
{
    private final int maxSize;
    private final Object[] array;

    // These values save the current elements.
    private int head = 0;
    private int tail = 0;
    private int size = 0;

    /**
     * Create a new {@link ContinuousArrayBuffer}.
     *
     * @param maxSize the size of the internal array to store the data. Once it is full, the oldest element will be
     *                overwritten.
     */
    public ContinuousArrayBuffer(int maxSize)
    {
        this.maxSize = maxSize;
        this.array = new Object[maxSize];
    }

    @Override
    public int getMaxSize()
    {
        return this.maxSize;
    }

    @Override
    public boolean bufferObject(T object)
    {
        // First run through
        if (this.size < maxSize) {
            this.size++;
        }
        // Now the array is already full.
        // This means we need to handle the tail.
        else {
            this.onForget((T) array[tail]);
            tail = incrementIndexSafely(tail);
        }

        array[head] = object;
        head = incrementIndexSafely(head);
        return false;
    }

    @Override
    public int size()
    {
        return this.size;
    }

    @Override
    public boolean isEmpty()
    {
        return this.size == 0;
    }

    @Override
    public void clear()
    {
        // We don't need to specifically clear the array as the values will be overwritten anyways.
        this.head = 0;
        this.tail = 0;
        this.size = 0;
    }

    @Override
    public void forceClear()
    {
        this.clear();
        Arrays.fill(this.array, null);
    }

    @Override
    public void clearIteration(Consumer<T> consumer)
    {
        this.forEach(consumer);
        this.clear();
    }


    @Override
    public void clearDescendingIteration(Consumer<T> consumer)
    {
        final Iterator<T> descendingIterator = this.descendingIterator();
        while (descendingIterator.hasNext()) {
            consumer.accept(descendingIterator.next());
        }

        this.clear();
    }

    @NotNull
    @Override
    public Iterator<T> iterator()
    {
        return new Iterator<T>()
        {
            // Start one before tail so next will return tail.
            private int currentIndex = tail;

            @Override
            public boolean hasNext()
            {
                return currentIndex != head;
            }

            @Override
            public T next()
            {
                T returnedObject = (T) array[currentIndex];
                currentIndex = incrementIndexSafely(currentIndex);
                return returnedObject;
            }
        };
    }


    @Override
    public Iterator<T> descendingIterator()
    {
        return new Iterator<T>()
        {
            // Start one before tail so next will return tail.
            private int currentIndex = head;

            @Override
            public boolean hasNext()
            {
                return currentIndex != tail;
            }

            @Override
            public T next()
            {
                currentIndex = decrementIndexSafely(currentIndex);
                return (T) array[currentIndex];
            }
        };
    }

    /**
     * Increments an index safely, so that it starts again at 0 if it is equal to or surpasses the maxSize and returns
     * that new value.
     */
    private int incrementIndexSafely(int index)
    {
        if (++index >= maxSize) {
            index = 0;
        }
        return index;
    }

    /**
     * Increments an index safely, so that it starts again at 0 if it is equal to or surpasses the maxSize and returns
     * that new value.
     */
    private int decrementIndexSafely(int index)
    {
        if (--index < 0) {
            index = maxSize - 1;
        }
        return index;
    }
}

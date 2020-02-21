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
    /**
     * Newest element
     */
    private int head = 0;

    /**
     * Oldest element
     */
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
        // Initial state
        if (this.size == 0) {
            this.size++;
        }
        // First run through
        else if (this.size < maxSize) {
            head = incrementIndexSafely(head);
            this.size++;
        }
        // Now the array is already full.
        // This means we need to handle the tail.
        else {
            head = incrementIndexSafely(head);

            this.onForget((T) array[tail]);
            tail = incrementIndexSafely(tail);
        }
        array[head] = object;

        System.out.println("HEAD: " + head + " TAIL " + tail);
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
        System.out.println("ITERATOR HEAD: " + head + " TAIL " + tail);

        return new Iterator<T>()
        {
            // Start makes sure that we can start the iteration even when the incremented head is the tail.
            private boolean start = true;
            private int currentIndex = tail;

            @Override
            public boolean hasNext()
            {
                // + 1 to make sure the last element will be included in the iteration.
                return start || currentIndex != incrementIndexSafely(head);
            }

            @Override
            public T next()
            {
                start = false;
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
            // Start makes sure that we can start the iteration even when the decremented tail is the head.
            private boolean start = true;
            private int currentIndex = head;

            @Override
            public boolean hasNext()
            {
                // - 1 to make sure the last element will be included in the iteration.
                return start || currentIndex != decrementIndexSafely(tail);
            }

            @Override
            public T next()
            {
                start = false;
                T returnedObject = (T) array[currentIndex];
                currentIndex = decrementIndexSafely(currentIndex);
                return returnedObject;
            }
        };
    }

    /**
     * Increments an index safely, so that it starts again at 0 if it is equal to or surpasses the maxSize and returns
     * that new value.
     */
    private int incrementIndexSafely(int index)
    {
        return (index + 1) % maxSize;
    }

    /**
     * Increments an index safely, so that it starts again at 0 if it is equal to or surpasses the maxSize and returns
     * that new value.
     */
    private int decrementIndexSafely(int index)
    {
        return index == 0 ? maxSize - 1 : index - 1;
    }
}

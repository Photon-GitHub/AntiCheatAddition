package de.photon.aacadditionpro.util.datastructure.buffer;

import de.photon.aacadditionpro.util.mathematics.ModularInteger;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

/**
 * An implementation of a ring buffer which overwrites the oldest data once it is full.
 * <p></p>
 * Note that the {@link RingBuffer} may be changed during iteration which will NOT cause the iteration to throw a
 * {@link java.util.ConcurrentModificationException}!
 * <p>
 * Make sure to properly synchronize access if iteration needs to be
 * stable.
 */
public class RingBuffer<T> implements FixedSizeBuffer<T>, Forgettable<T>
{
    private final int maxSize;
    private final T[] array;

    // The position at which the next element will be written.
    private ModularInteger head;

    // The position of the oldest element (if such an element exists).
    private ModularInteger tail;
    private int size = 0;

    /**
     * Create a new {@link de.photon.aacadditionproold.util.datastructures.buffer.ContinuousArrayBuffer}.
     *
     * @param maxSize the size of the internal array to store the data. Once it is full, the oldest element will be
     *                overwritten.
     */
    public RingBuffer(int maxSize)
    {
        this.maxSize = maxSize;
        this.array = (T[]) new Object[maxSize];
        this.head = new ModularInteger(0, maxSize);
        this.tail = new ModularInteger(0, maxSize);
    }

    @Override
    public int getMaxSize()
    {
        return this.maxSize;
    }

    @Override
    public boolean add(T elem)
    {
        if (this.size == maxSize) {
            this.onForget(array[tail.getAndIncrement()]);
        } else {
            ++this.size;
        }

        this.array[head.getAndIncrement()] = elem;
        return true;
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
        this.head.setToZero();
        this.tail.setToZero();
        this.size = 0;
    }

    @Override
    public void fullClear()
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
            private int index = tail.get();

            @Override
            public boolean hasNext()
            {
                return index != head.get();
            }

            @Override
            public T next()
            {
                if (!hasNext()) throw new NoSuchElementException();
                T elem = array[index];
                index = ModularInteger.increment(index, maxSize);
                return elem;
            }
        };
    }


    @Override
    public Iterator<T> descendingIterator()
    {
        return new Iterator<T>()
        {
            // Start at head - 1 as head is the position at which will be written next, but right now there is no
            // element there, or the oldest element.
            private int index = ModularInteger.decrement(head.get(), maxSize);

            @Override
            public boolean hasNext()
            {
                // Decrement as the tail itself shall be included in the iteration.
                return index != ModularInteger.decrement(tail.get(), maxSize);
            }

            @Override
            public T next()
            {
                if (!hasNext()) throw new NoSuchElementException();
                T elem = array[index];
                index = ModularInteger.decrement(index, maxSize);
                return elem;
            }
        };
    }
}

package de.photon.aacadditionpro.util.datastructure.buffer;

import de.photon.aacadditionpro.util.mathematics.ModularInteger;
import lombok.val;
import org.apache.commons.lang.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An implementation of a ring buffer which overwrites the oldest data once it is full.
 * <p></p>
 * Note that the {@link RingBuffer} may be changed during iteration which will NOT cause the iteration to throw a
 * {@link java.util.ConcurrentModificationException}! They are guaranteed to iterate at most maxSize elements.
 * <p>
 * Make sure to properly synchronize access if iteration needs to be
 * stable.
 */
public class RingBuffer<T> implements FixedSizeBuffer<T>, Forgettable<T>
{
    private final int maxSize;
    private final T[] array;

    // The position at which the next element will be written.
    private final ModularInteger head;

    // The position of the oldest element (if such an element exists).
    private final ModularInteger tail;
    private int size = 0;

    /**
     * Create a new {@link RingBuffer}.
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

    public RingBuffer(int maxSize, T defaultObject)
    {
        this(maxSize);
        Arrays.fill(array, defaultObject);
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

    public T head()
    {
        return this.array[ModularInteger.decrement(head.get(), maxSize)];
    }

    public T tail()
    {
        return this.array[tail.get()];
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
    public boolean contains(Object o)
    {
        return ArrayUtils.contains(this.array, o);
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

    @NotNull
    @Override
    public Iterator<T> iterator()
    {
        return new Iterator<T>()
        {
            private int index = tail.get();
            // An element counter to limit the iterated elements.
            private int elements = 0;

            @Override
            public boolean hasNext()
            {
                return elements < maxSize;
            }

            @Override
            public T next()
            {
                if (!hasNext()) throw new NoSuchElementException();
                T elem = array[index];
                index = ModularInteger.increment(index, maxSize);
                ++elements;
                return elem;
            }
        };
    }

    @NotNull
    @Override
    public Object @NotNull [] toArray()
    {
        val elements = new Object[this.size];
        int i = 0;
        for (T t : this) elements[i++] = t;
        return elements;
    }

    @NotNull
    @Override
    public <T1> T1 @NotNull [] toArray(T1[] a)
    {
        final T1[] elements = a.length < size ? (T1[]) Array.newInstance(a.getClass().getComponentType(), size) : a;
        int i = 0;
        for (T t : this) elements[i++] = (T1) t;
        if (a.length > size) elements[size - 1] = null;
        return elements;
    }


    @Override
    public Iterator<T> descendingIterator()
    {
        return new Iterator<T>()
        {
            // Start at head - 1 as head is the position at which will be written next, but right now there is no
            // element there, or the oldest element.
            private int index = ModularInteger.decrement(head.get(), maxSize);
            // An element counter to limit the iterated elements.
            private int elements = 0;

            @Override
            public boolean hasNext()
            {
                return elements < maxSize;
            }

            @Override
            public T next()
            {
                if (!hasNext()) throw new NoSuchElementException();
                T elem = array[index];
                index = ModularInteger.decrement(index, maxSize);
                ++elements;
                return elem;
            }
        };
    }
}

package de.photon.anticheataddition.util.datastructure.buffer;

import de.photon.anticheataddition.util.mathematics.ModularInteger;
import lombok.Getter;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;

/**
 * A ring buffer that overwrites the oldest data once it is full.
 * This is NOT thread-safe.
 */
public class RingBuffer<T> extends AbstractList<T> implements List<T>
{
    @Getter
    private final int maxSize;
    private final T[] array;

    // Pointer for the next index to be written.
    private final ModularInteger head;
    // Pointer for the oldest element.
    private final ModularInteger tail;
    private int size = 0;

    /**
     * Create a new RingBuffer.
     *
     * @param maxSize the maximum number of elements; once full, the oldest element will be overwritten.
     */
    public RingBuffer(int maxSize)
    {
        this.maxSize = maxSize;
        this.array = (T[]) new Object[maxSize];
        this.head = new ModularInteger(0, maxSize);
        this.tail = new ModularInteger(0, maxSize);
    }

    /**
     * Create a new RingBuffer with all elements pre-filled.
     *
     * @param maxSize       the maximum number of elements.
     * @param defaultObject the default object to fill the buffer with.
     */
    public RingBuffer(int maxSize, T defaultObject)
    {
        this(maxSize);
        Arrays.fill(array, defaultObject);
        this.size = maxSize;
    }

    @Override
    public boolean add(T elem)
    {
        // If full, overwrite the oldest element.
        if (this.size == maxSize) onForget(array[tail.getAndIncrement()]);
        else ++this.size;

        // Write element at head and then increment head.
        array[head.getAndIncrement()] = elem;
        return true;
    }

    @Override
    public T get(int index)
    {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("Index " + index + " out of bounds for size " + size);
        // Tail is the oldest element, so it will be element 0.
        return array[(tail.get() + index) % maxSize];
    }

    @Override
    public void clear()
    {
        // Clear the reference to allow garbage collection.
        Arrays.fill(array, null);

        head.set(0);
        tail.set(0);
        size = 0;
    }

    /**
     * Callback invoked when an element is overwritten/forgotten.
     * <p>
     * Subclasses can override this to handle removal events.
     */
    protected void onForget(T t)
    {
        // Default implementation does nothing.
    }

    @Override
    public T removeFirst()
    {
        if (size == 0) throw new IllegalStateException("Buffer is empty");
        final int idx = tail.getAndIncrement();
        final T elem = array[idx];
        onForget(elem);
        // Clear the reference to allow garbage collection.
        array[idx] = null;
        --this.size;
        return elem;
    }

    @Override
    public T removeLast()
    {
        if (size == 0) throw new IllegalStateException("Buffer is empty");
        // Decrement head to get the index of the last element.
        head.decrement();
        final int idx = head.get();
        final T elem = array[idx];
        onForget(elem);
        // Clear the reference to allow garbage collection.
        array[idx] = null;
        --this.size;
        return elem;
    }

    @Override
    public int size()
    {
        return this.size;
    }
}

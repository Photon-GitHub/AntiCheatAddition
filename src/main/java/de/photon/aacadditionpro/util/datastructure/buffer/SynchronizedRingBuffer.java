package de.photon.aacadditionpro.util.datastructure.buffer;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * A synchronized {@link RingBuffer} which is thread-safe.
 */
public class SynchronizedRingBuffer<T> extends RingBuffer<T>
{
    public SynchronizedRingBuffer(int maxSize)
    {
        super(maxSize);
    }

    public SynchronizedRingBuffer(int maxSize, T defaultObject)
    {
        super(maxSize, defaultObject);
    }

    @Override
    public boolean add(T elem)
    {
        synchronized (this) {
            return super.add(elem);
        }
    }

    @Override
    public void clear()
    {
        synchronized (this) {
            super.clear();
        }
    }

    @Override
    public void fullClear()
    {
        synchronized (this) {
            super.fullClear();
        }
    }

    @Override
    public @NotNull Iterator<T> iterator()
    {
        synchronized (this) {
            return super.iterator();
        }
    }

    @Override
    public Iterator<T> descendingIterator()
    {
        synchronized (this) {
            return super.descendingIterator();
        }
    }

    @Override
    public void clearIteration(Consumer<? super T> action)
    {
        synchronized (this) {
            super.clearIteration(action);
        }
    }

    @Override
    public void clearDescendingIteration(Consumer<? super T> action)
    {
        synchronized (this) {
            super.clearDescendingIteration(action);
        }
    }
}

package de.photon.AACAdditionPro.util.datastructures.buffer;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

public class SimpleBuffer<T> implements DequeBuffer<T>
{
    private final int capacity;
    @Getter
    private final Deque<T> deque;

    public SimpleBuffer(int capacity)
    {
        this.capacity = capacity;
        this.deque = new ArrayDeque<>(this.capacity);
    }

    public boolean bufferObject(final T object)
    {
        this.deque.push(object);
        return this.hasReachedBufferSize();
    }

    @Override
    public void clear()
    {
        this.deque.clear();
    }

    @Override
    public Iterator<T> descendingIterator()
    {
        return deque.descendingIterator();
    }

    @Override
    public int size()
    {
        return deque.size();
    }

    @Override
    public boolean isEmpty()
    {
        return deque.isEmpty();
    }

    public boolean hasReachedBufferSize()
    {
        return this.deque.size() >= this.capacity;
    }

    @NotNull
    @Override
    public Iterator<T> iterator()
    {
        return deque.iterator();
    }
}

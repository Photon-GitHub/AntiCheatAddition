package de.photon.AACAdditionPro.util.datastructures;

import lombok.Getter;

import java.util.ArrayDeque;
import java.util.Deque;

public class SimpleBuffer<T> implements Buffer<T>
{
    private final int capacity;
    @Getter
    private Deque<T> deque;

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

    public boolean hasReachedBufferSize()
    {
        return this.deque.size() >= this.capacity;
    }
}

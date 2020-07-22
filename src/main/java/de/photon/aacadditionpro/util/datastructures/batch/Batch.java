package de.photon.aacadditionpro.util.datastructures.batch;

import com.google.common.collect.ImmutableList;
import de.photon.aacadditionpro.user.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Batch<T>
{
    private final User user;
    private final T[] values;
    private final int capacity;
    private final Set<BatchProcessor<T>> processors = new HashSet<>();
    private volatile int index;
    private T lastAdded;

    public Batch(User user, int capacity, T dummyLastAdded)
    {
        this.user = user;
        this.capacity = capacity;
        this.values = (T[]) new Object[capacity];
        this.lastAdded = dummyLastAdded;
    }

    public synchronized void addDataPoint(T value)
    {
        this.lastAdded = value;
        this.values[this.index++] = value;

        if (this.index >= this.capacity) {
            final List<T> list = ImmutableList.copyOf(this.values);

            for (BatchProcessor<T> processor : processors) {
                processor.submit(this.user, list);
            }
            this.index = 0;
        }
    }

    public synchronized T peekLastAdded()
    {
        return lastAdded;
    }

    public synchronized void clear()
    {
        this.index = 0;
    }

    public synchronized void registerProcessor(BatchProcessor<T> processor)
    {
        this.processors.add(processor);
    }

    public synchronized void unregisterProcessor(BatchProcessor<T> processor)
    {
        this.processors.remove(processor);
    }
}

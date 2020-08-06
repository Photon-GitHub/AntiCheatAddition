package de.photon.aacadditionpro.util.datastructures.batch;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import de.photon.aacadditionpro.user.User;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A thread safe class to save up a certain amount to elements which are then processed by {@link BatchProcessor}s.
 */
public class Batch<T>
{
    private final User user;
    private final T[] values;
    private final int capacity;
    private final Set<BatchProcessor<T>> processors = new HashSet<>();

    private volatile int index = 0;
    // Volatile is ok here as we do not change the object itself and only care for the reference.
    private T lastAdded;

    public Batch(User user, int capacity, T dummyLastAdded)
    {
        Preconditions.checkArgument(capacity > 0, "Invalid batch size specified.");

        this.user = user;
        this.capacity = capacity;
        this.values = (T[]) new Object[capacity];
        this.lastAdded = Preconditions.checkNotNull(dummyLastAdded, "Tried to create batch without dummy.");
    }

    /**
     * This will add a datapoint to the {@link Batch}.
     */
    public synchronized void addDataPoint(T value)
    {
        this.lastAdded = value;
        this.values[this.index++] = value;

        if (this.index >= this.capacity) {
            final List<T> list = ImmutableList.copyOf(this.values);

            for (BatchProcessor<T> processor : processors) {
                processor.submit(this.user, list);
            }

            this.clear();
        }
    }

    /**
     * This will return the most recently added element.
     * As a {@link Batch} is always initialized with a non-null dummy element, this method will always return a non-null
     * value.
     */
    @NotNull
    public T peekLastAdded()
    {
        return lastAdded;
    }

    /**
     * Clears the {@link Batch} by setting the write index to 0.
     * This will make any newly added datapoints overwrite the currently present data.
     */
    public void clear()
    {
        // No synchronized is needed as we only perform one write operation.
        this.index = 0;
    }

    /**
     * Register a {@link BatchProcessor} which shall receive a copy of the {@link Batch} data once the {@link Batch}
     * capacity is reached.
     */
    public synchronized void registerProcessor(@NotNull BatchProcessor<T> processor)
    {
        this.processors.add(processor);
    }

    /**
     * Unregister a {@link BatchProcessor}.
     */
    public synchronized void unregisterProcessor(@NotNull BatchProcessor<T> processor)
    {
        this.processors.remove(processor);
    }
}

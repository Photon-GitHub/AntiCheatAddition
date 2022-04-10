package de.photon.anticheataddition.util.datastructure.batch;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.datastructure.broadcast.Broadcaster;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * A thread safe class to save up a certain amount to elements which are then processed by {@link BatchProcessor}s.
 */
public class Batch<T>
{
    @NotNull private final User user;
    @NotNull private final T[] values;
    @NotNull private final Broadcaster<Snapshot<T>> broadcaster;

    private int index = 0;
    // Volatile is ok here as we do not change the object itself and only care for the reference.
    @NotNull private T lastAdded;

    public Batch(Broadcaster<Snapshot<T>> broadcaster, User user, int capacity, T dummyLastAdded)
    {
        Preconditions.checkArgument(capacity > 0, "Invalid batch size specified.");
        this.broadcaster = Preconditions.checkNotNull(broadcaster, "Tried to create batch with null broadcaster.");
        this.user = Preconditions.checkNotNull(user, "Tried to create batch with null user.");
        this.values = (T[]) new Object[capacity];
        this.lastAdded = Preconditions.checkNotNull(dummyLastAdded, "Tried to create batch with null dummy.");
    }

    /**
     * This will add a datapoint to the {@link Batch}.
     */
    public synchronized void addDataPoint(T value)
    {
        Preconditions.checkNotNull(value, "Tried to add null value to batch");
        this.lastAdded = value;
        this.values[this.index++] = value;

        if (this.index >= this.values.length) {
            broadcaster.broadcast(Snapshot.of(this));
            // Clear the batch.
            this.clear();
        }
    }

    /**
     * This will return the most recently added element.
     * As a {@link Batch} is always initialized with a non-null dummy element, this method will always return a non-null
     * value.
     */
    @NotNull
    public synchronized T peekLastAdded()
    {
        return lastAdded;
    }

    /**
     * Clears the {@link Batch} by setting the write-index to 0.
     * This will make any newly added data-points overwrite the currently present data.
     */
    public synchronized void clear()
    {
        // No synchronization is needed as we only perform one write operation.
        this.index = 0;
    }

    /**
     * Represents a snapshot of a {@link Batch}, e.g. for broadcasting.
     */
    @Value
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Snapshot<T>
    {
        @NotNull User user;
        @NotNull @Unmodifiable List<T> values;

        protected static <T> Snapshot<T> of(@NotNull Batch<T> batch)
        {
            return new Snapshot<>(batch.user, List.of(batch.values));
        }
    }
}
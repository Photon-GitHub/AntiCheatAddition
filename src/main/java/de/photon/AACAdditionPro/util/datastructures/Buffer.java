package de.photon.AACAdditionPro.util.datastructures;

import java.util.Deque;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface Buffer<T>
{
    Deque<T> getDeque();

    /**
     * Adds an {@link Object} of type {@link T} to the buffer
     *
     * @param object The object which should be added.
     *
     * @return true if the size of the buffer is bigger than the max_size.
     */
    boolean bufferObject(final T object);

    boolean hasReachedBufferSize();

    /**
     * Iterates through the buffer and clears it at the same time.
     *
     * @param lastObjectConsumer the code which should run for each element.
     */
    default void clearLastObjectIteration(final Consumer<T> lastObjectConsumer)
    {
        while (!this.getDeque().isEmpty()) {
            lastObjectConsumer.accept(this.getDeque().pop());
        }
    }

    /**
     * Iterates through the buffer and clears it at the same time
     * The most recently added element will always be in last.
     * After one cycle the current object will be written to last.
     *
     * @param lastObjectsConsumer the code which should be run in each cycle, consuming the last and the current object.
     */
    default void clearLastTwoObjectsIteration(final BiConsumer<T, T> lastObjectsConsumer)
    {
        if (!this.getDeque().isEmpty()) {
            T last = this.getDeque().pop();
            T current;
            while (!this.getDeque().isEmpty()) {
                current = this.getDeque().pop();
                lastObjectsConsumer.accept(last, current);
                last = current;
            }
        }
    }
}

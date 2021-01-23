package de.photon.aacadditionproold.util.datastructures.buffer;

import java.util.Deque;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface DequeBuffer<T> extends Buffer<T>
{
    Deque<T> getDeque();

    /**
     * Determines whether the {@link Deque} has at least one element.
     */
    default boolean dequeHasAnElement()
    {
        return !this.getDeque().isEmpty();
    }

    @Override
    default void clearIteration(final Consumer<T> consumer)
    {
        // We use .push as buffer method, which is equal to addFirst. Therefore removeLast correctly returns the least
        // recently added element here.
        while (this.dequeHasAnElement()) {
            consumer.accept(this.getDeque().removeLast());
        }
    }

    @Override
    default void clearDescendingIteration(final Consumer<T> consumer)
    {
        // We use .push as buffer method, which is equal to addFirst. Therefore removeFirst correctly returns the most
        // recently added element here.
        while (this.dequeHasAnElement()) {
            consumer.accept(this.getDeque().removeFirst());
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
        if (this.dequeHasAnElement()) {
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

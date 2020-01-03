package de.photon.aacadditionpro.util.datastructures.buffer;


import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Represents a way of storing data of arbitrary type and working with it.
 */
public interface Buffer<T> extends Iterable<T>
{
    /**
     * Adds an {@link Object} of type {@link T} to the {@link Buffer}
     *
     * @param object The object which should be added.
     *
     * @return true if the size of the {@link Buffer} is bigger than the max_size.
     */
    boolean bufferObject(final T object);

    /**
     * Clears the {@link Buffer}
     */
    void clear();

    /**
     * Clears the buffer starting from the least recently added element and performs an action on each element.
     */
    void clearIteration(final Consumer<T> consumer);

    /**
     * Clears the buffer starting from the most recently added element and performs an action on each element.
     */
    void clearDescendingIteration(final Consumer<T> consumer);

    /**
     * Creates an {@link Iterator} that starts with the most recently added element.
     */
    Iterator<T> descendingIterator();

    /**
     * @return the current amount of elements the {@link Buffer} holds.
     */
    int size();

    /**
     * @return whether or not the {@link Buffer} is empty.
     */
    boolean isEmpty();
}

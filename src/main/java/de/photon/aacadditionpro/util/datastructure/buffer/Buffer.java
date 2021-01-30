package de.photon.aacadditionpro.util.datastructure.buffer;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Represents a way of storing data of arbitrary type and working with it.
 */
public interface Buffer<T> extends Iterable<T>
{
    /**
     * Adds an {@link Object} to the {@link Buffer}
     *
     * @param object The object which should be added.
     *
     * @return true if the buffer changed because of calling this method.
     */
    boolean add(T object);

    /**
     * @return whether or not the {@link Buffer} is empty.
     */
    boolean isEmpty();

    /**
     * @return the current amount of elements the {@link Buffer} holds.
     */
    int size();

    /**
     * Clears the {@link Buffer}
     */
    void clear();

    /**
     * Creates an {@link Iterator} that starts with the most recently added element.
     */
    Iterator<T> descendingIterator();

    /**
     * Clears the buffer starting from the least recently added element and performs an action on each element.
     */
    void clearIteration(Consumer<T> consumer);

    /**
     * Clears the buffer starting from the most recently added element and performs an action on each element.
     */
    void clearDescendingIteration(Consumer<T> consumer);
}

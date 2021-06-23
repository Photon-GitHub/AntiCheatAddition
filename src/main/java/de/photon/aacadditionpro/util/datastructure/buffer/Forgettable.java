package de.photon.aacadditionpro.util.datastructure.buffer;

/**
 * Represents a collection that can forget items.
 */
public interface Forgettable<T>
{
    default void onForget(T forgotten) {}
}

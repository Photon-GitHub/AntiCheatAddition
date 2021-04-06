package de.photon.aacadditionpro.user.data;

/**
 * A registry-like map to store data for well-defined entries.
 * <p>
 * The map shall be threadsafe.
 */
public interface DataMap<T extends Enum<T>, V> extends ImmutableDataMap<T, V>
{
    /**
     * Sets a value in the map.
     */
    void setValue(T key, V value);

    /**
     * Clears the map.
     */
    void clear();
}

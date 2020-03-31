package de.photon.aacadditionpro.user;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * A registry-like map to store data for well-defined entries.
 * The underlying structure is an {@link EnumMap}.
 * <p>
 * The map is synchronized via {@link Collections#synchronizedMap(Map)}.
 */
public class DataMap<T extends Enum<T>, V>
{
    protected final Map<T, V> map;

    /**
     * Creates a new DataMap.
     */
    public DataMap(Class<T> enumeration)
    {
        this.map = Collections.synchronizedMap(new EnumMap<>(enumeration));
    }

    /**
     * Gets a value from the map.
     */
    public V getValue(T key)
    {
        return this.map.get(key);
    }

    /**
     * Sets a value in the map.
     */
    public void setValue(T key, V value)
    {
        this.map.put(key, value);
    }

    /**
     * Clears the map.
     */
    public void clear()
    {
        this.map.clear();
    }
}

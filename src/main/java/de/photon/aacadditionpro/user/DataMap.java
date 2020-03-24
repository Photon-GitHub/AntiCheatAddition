package de.photon.aacadditionpro.user;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public class DataMap<T extends Enum<T>, V>
{
    protected final Map<T, V> map;

    public DataMap(Class<T> enumeration)
    {
        this.map = Collections.synchronizedMap(new EnumMap<>(enumeration));
    }

    public V getValue(T key)
    {
        return this.map.get(key);
    }

    public void setValue(T key, V value)
    {
        this.map.put(key, value);
    }

    public void clear()
    {
        this.map.clear();
    }
}

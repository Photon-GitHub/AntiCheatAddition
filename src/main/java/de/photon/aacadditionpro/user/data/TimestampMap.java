package de.photon.aacadditionpro.user.data;

import lombok.val;

import java.util.EnumMap;
import java.util.Map;

public class TimestampMap
{
    // As the map is not modified by a single thread, we can safely read without synchronization.
    private final Map<TimestampKey, Timestamp> map;

    public TimestampMap()
    {
        val enumMap = new EnumMap<TimestampKey, Timestamp>(TimestampKey.class);
        for (TimestampKey value : TimestampKey.values()) enumMap.put(value, new Timestamp());
        this.map = enumMap;
    }

    public Timestamp getValue(TimestampKey key)
    {
        return map.get(key);
    }
}

package de.photon.anticheataddition.user.data;

import lombok.val;

import java.util.EnumMap;
import java.util.Map;

public final class TimestampMap
{
    // As the map is not modified by a single thread, we can safely read without synchronization.
    private final Map<TimeKey, Timestamp> map;

    public TimestampMap()
    {
        val enumMap = new EnumMap<TimeKey, Timestamp>(TimeKey.class);
        for (TimeKey value : TimeKey.values()) enumMap.put(value, new Timestamp());
        this.map = enumMap;
    }

    public Timestamp at(TimeKey key)
    {
        return map.get(key);
    }
}

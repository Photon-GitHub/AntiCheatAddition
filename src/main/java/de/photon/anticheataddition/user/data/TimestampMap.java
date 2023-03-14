package de.photon.anticheataddition.user.data;

public final class TimestampMap
{
    // We know that every enum has a non-null Timestamp as a value, so we can use an array instead of a map.
    private final Timestamp[] values;

    public TimestampMap()
    {
        values = new Timestamp[TimeKey.values().length];
        for (int i = 0; i < values.length; ++i) values[i] = new Timestamp();
    }

    public Timestamp at(TimeKey key)
    {
        return values[key.ordinal()];
    }
}

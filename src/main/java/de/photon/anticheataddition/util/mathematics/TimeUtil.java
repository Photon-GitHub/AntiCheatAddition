package de.photon.anticheataddition.util.mathematics;

import lombok.experimental.UtilityClass;

import java.util.concurrent.TimeUnit;

@UtilityClass
public class TimeUtil
{
    public static long toMillis(long ticks)
    {
        return ticks * 50;
    }

    public static long toUnit(TimeUnit resultUnit, long ticks)
    {
        return resultUnit.convert(toMillis(ticks), TimeUnit.MILLISECONDS);
    }

    public static long toTicks(long millis)
    {
        return millis / 50;
    }

    public static long toTicks(TimeUnit previousUnit, long units)
    {
        return toTicks(previousUnit.toMillis(units));
    }
}

package de.photon.anticheataddition.util.mathematics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

class TimeUtilTest
{
    @Test
    void ticksToUnitTest()
    {
        Assertions.assertEquals(1, TimeUtil.toUnit(TimeUnit.SECONDS, 20));
        Assertions.assertEquals(1000, TimeUtil.toUnit(TimeUnit.MILLISECONDS, 20));
        Assertions.assertEquals(1, TimeUtil.toUnit(TimeUnit.MINUTES, 20 * 60));
    }

    @Test
    void unitToTicksTest()
    {
        Assertions.assertEquals(20, TimeUtil.toTicks(1, TimeUnit.SECONDS));
        Assertions.assertEquals(1, TimeUtil.toTicks(50, TimeUnit.MILLISECONDS));
        Assertions.assertEquals(20 * 60, TimeUtil.toTicks(1, TimeUnit.MINUTES));
    }
}

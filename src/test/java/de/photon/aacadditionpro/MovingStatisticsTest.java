package de.photon.aacadditionpro;


import de.photon.aacadditionpro.util.datastructure.statistics.MovingDoubleStatistics;
import de.photon.aacadditionpro.util.datastructure.statistics.MovingLongStatistics;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

class MovingStatisticsTest
{
    private static final double DELTA = 0.001;

    @Test
    void MovingStatisticsNegativeTest()
    {
        IntStream.of(Integer.MIN_VALUE, -575442466, -4545645, -65423, -1521, -154, -2, -1, 0).forEach(negative -> {
            Assertions.assertThrows(IllegalArgumentException.class, () -> new MovingDoubleStatistics(negative), "Can create negative or 0 capacity MovingDoubleStatistics");
            Assertions.assertThrows(IllegalArgumentException.class, () -> new MovingDoubleStatistics(negative, negative), "Can create negative or 0 capacity MovingDoubleStatistics");
            Assertions.assertThrows(IllegalArgumentException.class, () -> new MovingLongStatistics(negative), "Can create negative or 0 capacity MovingLongStatistics");
            Assertions.assertThrows(IllegalArgumentException.class, () -> new MovingLongStatistics(negative, negative), "Can create negative or 0 capacity MovingDoubleStatistics");
        });
    }

    @Test
    void MovingStatisticsDefaultOverflowTest()
    {
        Assertions.assertThrows(ArithmeticException.class, () -> new MovingLongStatistics(2, Long.MAX_VALUE), "Can create default overflowing MovingLongStatistics");
        Assertions.assertThrows(ArithmeticException.class, () -> new MovingLongStatistics(2, Long.MIN_VALUE), "Can create default overflowing MovingLongStatistics");
    }

    @Test
    void MovingStatisticsNoDefaultTest()
    {
        val capacity = 10;

        val doubleStats = new MovingDoubleStatistics(capacity);
        val longStats = new MovingLongStatistics(capacity);

        Assertions.assertEquals(0, doubleStats.getSum());
        Assertions.assertEquals(0, longStats.getSum());

        Assertions.assertEquals(0, doubleStats.getAverage());
        Assertions.assertEquals(0, longStats.getAverage());

        doubleStats.add(capacity);
        longStats.add(capacity);

        Assertions.assertEquals(capacity, doubleStats.getSum(), DELTA, "Wrong sum.");
        Assertions.assertEquals(capacity, longStats.getSum(), "Wrong sum.");

        Assertions.assertEquals(1, doubleStats.getAverage(), DELTA, "Wrong average.");
        Assertions.assertEquals(1, longStats.getAverage(), DELTA, "Wrong average.");

        // Fill up the statistics, so all values are overwritten.
        for (int i = 0; i < capacity; ++i) {
            doubleStats.add(2);
            longStats.add(2);
        }

        Assertions.assertEquals(2 * capacity, doubleStats.getSum(), DELTA, "Wrong sum.");
        Assertions.assertEquals(2 * capacity, longStats.getSum(), "Wrong sum.");

        Assertions.assertEquals(2, doubleStats.getAverage(), DELTA, "Wrong average.");
        Assertions.assertEquals(2, longStats.getAverage(), DELTA, "Wrong average.");
    }

    @Test
    void MovingStatisticsDefaultTest()
    {
        val capacity = 10;
        val defaultValue = 100;

        val doubleStats = new MovingDoubleStatistics(capacity, defaultValue);
        val longStats = new MovingLongStatistics(capacity, defaultValue);

        Assertions.assertEquals(capacity * defaultValue, doubleStats.getSum(), DELTA, "Wrong sum.");
        Assertions.assertEquals(capacity * defaultValue, longStats.getSum(), "Wrong sum.");

        Assertions.assertEquals(defaultValue, doubleStats.getAverage(), DELTA, "Wrong average.");
        Assertions.assertEquals(defaultValue, longStats.getAverage(), DELTA, "Wrong average.");

        // Nothing should change by adding another default value.
        doubleStats.add(defaultValue);
        longStats.add(defaultValue);

        Assertions.assertEquals(capacity * defaultValue, doubleStats.getSum(), DELTA, "Wrong sum.");
        Assertions.assertEquals(capacity * defaultValue, longStats.getSum(), "Wrong sum.");

        Assertions.assertEquals(defaultValue, doubleStats.getAverage(), DELTA, "Wrong average.");
        Assertions.assertEquals(defaultValue, longStats.getAverage(), DELTA, "Wrong average.");

        doubleStats.add(0);
        longStats.add(0);

        Assertions.assertEquals((capacity * defaultValue) - defaultValue, doubleStats.getSum(), DELTA, "Wrong sum.");
        Assertions.assertEquals((capacity * defaultValue) - defaultValue, longStats.getSum(), "Wrong sum.");

        Assertions.assertEquals(defaultValue - ((double) defaultValue / capacity), doubleStats.getAverage(), DELTA, "Wrong average.");
        Assertions.assertEquals(defaultValue - ((double) defaultValue / capacity), longStats.getAverage(), DELTA, "Wrong average.");

        // Fill up the statistics, so all values are overwritten.
        for (int i = 0; i < capacity; ++i) {
            doubleStats.add(2);
            longStats.add(2);
        }

        Assertions.assertEquals(2 * capacity, doubleStats.getSum(), DELTA, "Wrong sum.");
        Assertions.assertEquals(2 * capacity, longStats.getSum(), "Wrong sum.");

        Assertions.assertEquals(2, doubleStats.getAverage(), DELTA, "Wrong average.");
        Assertions.assertEquals(2, longStats.getAverage(), DELTA, "Wrong average.");
    }
}

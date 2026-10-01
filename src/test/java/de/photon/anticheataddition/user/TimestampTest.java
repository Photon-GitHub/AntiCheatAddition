package de.photon.anticheataddition.user;

import de.photon.anticheataddition.user.data.Timestamp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TimestampTest
{
    private static void expectZero(Timestamp timestamp)
    {
        Assertions.assertEquals(0, timestamp.getTime());
        Assertions.assertTrue(timestamp.passedTime() > 0, "The passed time should be greater than 0.");
        Assertions.assertFalse(timestamp.recentlyUpdated(100000));
        Assertions.assertTrue(timestamp.notRecentlyUpdated(100000));
    }

    @Test
    void zeroTest()
    {
        final var timestamp = new Timestamp();
        expectZero(timestamp);

        timestamp.update();
        Assertions.assertNotEquals(0, timestamp.getTime(), "The time should be initialized.");
        timestamp.setToZero();

        expectZero(timestamp);
    }

    @Test
    void futureTest()
    {
        final var timestamp = new Timestamp();
        timestamp.setToFuture(100000);
        Assertions.assertTrue(timestamp.passedTime() < 0, "The passed time should be less than 0 when the time is set to the future.");
        Assertions.assertTrue(timestamp.recentlyUpdated(0));
    }

    @Test
    void nanosecondsTest()
    {
        final var timestamp = new Timestamp();
        timestamp.update();

        Assertions.assertTrue(timestamp.passedNanos() >= 0);

        timestamp.setToZero();
        Assertions.assertEquals(0, timestamp.getTime());
    }
}

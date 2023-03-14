package de.photon.anticheataddition.user;

import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.user.data.TimestampMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

class TimestampMapTest
{
    final TimestampMap timestampMap = new TimestampMap();

    @Test
    void timestampMapTest()
    {
        // Init correct
        for (TimeKey key : TimeKey.values()) Assertions.assertNotNull(timestampMap.at(key));

        final var selectedKey = TimeKey.values()[ThreadLocalRandom.current().nextInt(TimeKey.values().length)];
        final var secondKey = selectedKey == TimeKey.values()[0] ? TimeKey.values()[1] : TimeKey.values()[0];

        timestampMap.at(selectedKey).update();
        timestampMap.at(secondKey).update();
        Assertions.assertTrue(timestampMap.at(secondKey).recentlyUpdated(100000));
        timestampMap.at(secondKey).setToZero();
        Assertions.assertTrue(timestampMap.at(selectedKey).recentlyUpdated(100000));
        Assertions.assertEquals(0, timestampMap.at(secondKey).getTime());
    }
}

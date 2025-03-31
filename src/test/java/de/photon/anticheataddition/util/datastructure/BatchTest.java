package de.photon.anticheataddition.util.datastructure;

import com.google.common.eventbus.EventBus;
import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.datastructure.batch.Batch;
import de.photon.anticheataddition.util.datastructure.batch.SyncBatchProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

class BatchTest
{
    private static final EventBus testBus = new EventBus();

    @BeforeAll
    static void setup()
    {
        Dummy.mockAntiCheatAddition();
    }

    @Test
    void dummyBatchTest()
    {
        final var dummy = Dummy.mockUser();
        Assertions.assertThrows(NullPointerException.class, () -> new Batch<>(testBus, dummy, 1, null));
        Assertions.assertThrows(NullPointerException.class, () -> new Batch<>(null, dummy, 1, "null"));
        Assertions.assertThrows(NullPointerException.class, () -> new Batch<String>(null, dummy, 1, null));
    }

    @Test
    void illegalCapacityBatchTest()
    {
        final var dummy = Dummy.mockUser();
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Batch<>(testBus, dummy, 0, ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Batch<>(testBus, dummy, -1, ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Batch<>(testBus, dummy, Integer.MIN_VALUE, ""));
    }

    @Test
    void peekingTest()
    {
        final int batchSize = 3;
        final var batch = new Batch<>(testBus, Dummy.mockUser(), batchSize, "");
        Assertions.assertEquals("", batch.peekLastAdded());

        batch.addDataPoint("SomeString");
        Assertions.assertEquals("SomeString", batch.peekLastAdded());
    }

    @Test
    void syncBatchProcessorTest()
    {
        final var expected = List.of("0", "1", "2", "3", "4", "5");
        final var output = new ArrayList<String>();
        final int batchSize = 3;

        final var batchProcessor = new SyncBatchProcessor<String>(Dummy.mockViolationModule("Inventory"), Set.of(testBus))
        {
            @Override
            public void processBatch(User user, List<String> batch)
            {
                output.addAll(batch);
            }
        };

        batchProcessor.enable();

        final var batch = new Batch<>(testBus, Dummy.mockUser(), batchSize, "");
        testBus.register(batchProcessor);

        for (int i = 0, n = 2 * batchSize; i < n; ++i) batch.addDataPoint(String.valueOf(i));

        Assertions.assertIterableEquals(expected, output);
        testBus.unregister(batchProcessor);

        for (int i = 0, n = 2 * batchSize; i < n; ++i) batch.addDataPoint("Test");

        Assertions.assertIterableEquals(expected, output);
    }
}

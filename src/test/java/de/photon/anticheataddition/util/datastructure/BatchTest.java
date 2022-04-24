package de.photon.anticheataddition.util.datastructure;

import com.google.common.eventbus.EventBus;
import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.datastructure.batch.AsyncBatchProcessor;
import de.photon.anticheataddition.util.datastructure.batch.Batch;
import de.photon.anticheataddition.util.datastructure.batch.SyncBatchProcessor;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class BatchTest
{
    private static final ViolationModule dummyVlModule = Dummy.mockViolationModule("Inventory");
    private static final EventBus testBus = new EventBus();

    // Do not remove this unused variable, it is needed for initialization of mocking.
    private static User dummy;

    @BeforeAll
    static void setup()
    {
        Dummy.mockAntiCheatAddition();
        dummy = Dummy.mockUser();
    }

    @Test
    void dummyBatchTest()
    {
        Assertions.assertThrows(NullPointerException.class, () -> new Batch<>(testBus, dummy, 1, null));
        Assertions.assertThrows(NullPointerException.class, () -> new Batch<>(null, dummy, 1, "null"));
        Assertions.assertThrows(NullPointerException.class, () -> new Batch<String>(null, dummy, 1, null));
    }

    @Test
    void illegalCapacityBatchTest()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Batch<>(testBus, dummy, 0, ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Batch<>(testBus, dummy, -1, ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Batch<>(testBus, dummy, Integer.MIN_VALUE, ""));
    }

    @Test
    void peekingTest()
    {
        final int batchSize = 3;
        val batch = new Batch<>(testBus, dummy, batchSize, "");
        Assertions.assertEquals("", batch.peekLastAdded());

        batch.addDataPoint("SomeString");
        Assertions.assertEquals("SomeString", batch.peekLastAdded());
    }

    @Test
    void syncBatchProcessorTest()
    {
        val dummyVlModule = new ViolationModule("Inventory")
        {
            @Override
            protected ViolationManagement createViolationManagement()
            {
                return null;
            }

            @Override
            protected ModuleLoader createModuleLoader()
            {
                return null;
            }
        };

        val output = new ArrayList<String>();
        final int batchSize = 3;

        val batchProcessor = new SyncBatchProcessor<String>(dummyVlModule, Set.of(testBus))
        {
            @Override
            public void processBatch(User user, List<String> batch)
            {
                output.addAll(batch);
            }
        };

        batchProcessor.enable();

        val batch = new Batch<>(testBus, dummy, batchSize, "");
        testBus.register(batchProcessor);

        for (int i = 0; i < 2 * batchSize; ++i) batch.addDataPoint(String.valueOf(i));

        Assertions.assertIterableEquals(List.of("0", "1", "2", "3", "4", "5"), output);

        testBus.unregister(batchProcessor);

        for (int i = 0; i < 2 * batchSize; ++i) {
            batch.addDataPoint("Test");
        }

        Assertions.assertIterableEquals(List.of("0", "1", "2", "3", "4", "5"), output);
    }

    @SneakyThrows
    @Test
    void asyncBatchProcessorTest()
    {
        val output = Collections.synchronizedList(new ArrayList<String>());
        final int batchSize = 3;

        val batchProcessor = new AsyncBatchProcessor<String>(dummyVlModule, Set.of(testBus))
        {
            @Override
            public void processBatch(User user, List<String> batch)
            {
                output.addAll(batch);
            }
        };

        batchProcessor.enable();

        val batch = new Batch<>(testBus, dummy, batchSize, "");
        testBus.register(batchProcessor);

        for (int i = 0; i < 6; ++i) batch.addDataPoint(String.valueOf(i));

        batchProcessor.controlledShutdown();

        // Make sure to respect the race condition.
        if ("0".equals(output.get(0))) {
            Assertions.assertIterableEquals(List.of("0", "1", "2", "3", "4", "5"), output);
        } else {
            Assertions.assertIterableEquals(List.of("3", "4", "5", "0", "1", "2"), output);
        }
    }
}

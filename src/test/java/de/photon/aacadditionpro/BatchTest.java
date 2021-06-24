package de.photon.aacadditionpro;

import com.google.common.collect.ImmutableList;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.datastructure.batch.AsyncBatchProcessor;
import de.photon.aacadditionpro.util.datastructure.batch.Batch;
import de.photon.aacadditionpro.util.datastructure.batch.SyncBatchProcessor;
import de.photon.aacadditionpro.util.datastructure.broadcast.Broadcaster;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.SneakyThrows;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class BatchTest
{
    private static final AACAdditionPro mock = Dummy.mockAACAdditionPro();
    private static final User dummy = Dummy.mockUser();
    private static final ViolationModule dummyVlModule = Dummy.mockViolationModule();
    private static final Broadcaster<Batch.Snapshot<String>> stringBroadcaster = new Broadcaster<>();

    @Test
    void dummyBatchTest()
    {
        Assertions.assertThrows(NullPointerException.class, () -> new Batch<>(stringBroadcaster, dummy, 1, null));
        Assertions.assertThrows(NullPointerException.class, () -> new Batch<>(null, dummy, 1, "null"));
        Assertions.assertThrows(NullPointerException.class, () -> new Batch<String>(null, dummy, 1, null));
    }

    @Test
    void illegalCapacityBatchTest()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Batch<>(stringBroadcaster, dummy, 0, ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Batch<>(stringBroadcaster, dummy, -1, ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Batch<>(stringBroadcaster, dummy, Integer.MIN_VALUE, ""));
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
        val batchSize = 3;

        val batchProcessor = new SyncBatchProcessor<String>(dummyVlModule, Collections.singleton(stringBroadcaster))
        {
            @Override
            public void processBatch(User user, List<String> batch)
            {
                output.addAll(batch);
            }
        };

        batchProcessor.enable();

        val batch = new Batch<>(stringBroadcaster, dummy, batchSize, "");
        stringBroadcaster.subscribe(batchProcessor);

        for (int i = 0; i < 6; ++i) batch.addDataPoint(String.valueOf(i));

        Assertions.assertIterableEquals(ImmutableList.of("0", "1", "2", "3", "4", "5"), output);
    }

    @SneakyThrows
    @Test
    void asyncBatchProcessorTest()
    {
        val output = Collections.synchronizedList(new ArrayList<String>());
        val batchSize = 3;

        val batchProcessor = new AsyncBatchProcessor<String>(dummyVlModule, Collections.singleton(stringBroadcaster))
        {
            @Override
            public void processBatch(User user, List<String> batch)
            {
                output.addAll(batch);
            }
        };

        batchProcessor.enable();

        val batch = new Batch<>(stringBroadcaster, dummy, batchSize, "");
        stringBroadcaster.subscribe(batchProcessor);

        for (int i = 0; i < 6; ++i) batch.addDataPoint(String.valueOf(i));

        batchProcessor.controlledShutdown();

        // Make sure to respect the race condition.
        if ("0".equals(output.get(0))) {
            Assertions.assertIterableEquals(ImmutableList.of("0", "1", "2", "3", "4", "5"), output);
        } else {
            Assertions.assertIterableEquals(ImmutableList.of("3", "4", "5", "0", "1", "2"), output);
        }
    }
}

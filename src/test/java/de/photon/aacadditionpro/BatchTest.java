package de.photon.aacadditionpro;

import com.google.common.collect.ImmutableList;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.datastructures.batch.AsyncBatchProcessor;
import de.photon.aacadditionpro.util.datastructures.batch.Batch;
import de.photon.aacadditionpro.util.datastructures.batch.SyncBatchProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class BatchTest
{
    @Test
    void dummyBatchTest()
    {
        Assertions.assertThrows(NullPointerException.class, () -> new Batch<String>(null, 1, null));
    }

    @Test
    void illegalCapacityBatchTest()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Batch<>(null, 0, ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Batch<>(null, -1, ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Batch<>(null, Integer.MIN_VALUE, ""));
    }

    @Test
    void syncBatchProcessorTest()
    {
        final List<String> output = new ArrayList<>();
        int batchSize = 3;

        final SyncBatchProcessor<String> batchProcessor = new SyncBatchProcessor<String>(batchSize)
        {
            @Override
            public void processBatch(User user, List<String> batch)
            {
                output.addAll(batch);
            }
        };
        Assertions.assertThrows(IllegalStateException.class, batchProcessor::disable);
        batchProcessor.enable();
        Assertions.assertThrows(IllegalStateException.class, batchProcessor::enable);

        Batch<String> batch = new Batch<>(null, batchSize, "");
        batch.registerProcessor(batchProcessor);

        for (int i = 0; i < 6; i++) {
            batch.addDataPoint(String.valueOf(i));
        }

        Assertions.assertIterableEquals(ImmutableList.of("0", "1", "2", "3", "4", "5"), output);
    }

    @Test
    void asyncBatchProcessorTest()
    {
        final List<String> output = new ArrayList<>();
        int batchSize = 3;

        final AsyncBatchProcessor<String> batchProcessor = new AsyncBatchProcessor<String>(batchSize)
        {
            @Override
            public void processBatch(User user, List<String> batch)
            {
                output.addAll(batch);
            }
        };
        Assertions.assertThrows(IllegalStateException.class, batchProcessor::disable);
        batchProcessor.enable();
        Assertions.assertThrows(IllegalStateException.class, batchProcessor::enable);

        Batch<String> batch = new Batch<>(null, batchSize, "");
        batch.registerProcessor(batchProcessor);

        for (int i = 0; i < 6; i++) {
            batch.addDataPoint(String.valueOf(i));
        }
        batchProcessor.controlledShutdown();

        // Make sure to respect the race condition.
        if (output.get(0).equals("0")) {
            Assertions.assertIterableEquals(ImmutableList.of("0", "1", "2", "3", "4", "5"), output);
        } else {
            Assertions.assertIterableEquals(ImmutableList.of("3", "4", "5", "0", "1", "2"), output);
        }
    }
}

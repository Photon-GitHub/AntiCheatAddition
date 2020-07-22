package de.photon.aacadditionpro;

import com.google.common.collect.ImmutableList;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.datastructures.batch.Batch;
import de.photon.aacadditionpro.util.datastructures.batch.BatchProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class BatchTest
{
    @Test
    public void dummyBatchTest()
    {
        Assertions.assertThrows(NullPointerException.class, () -> new Batch<String>(null, 1, null));
    }

    @Test
    public void illegalCapacityBatchTest()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Batch<>(null, 0, ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Batch<>(null, -1, ""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Batch<>(null, Integer.MIN_VALUE, ""));
    }

    @Test
    public void batchProcessorTest()
    {
        final List<String> output = new ArrayList<>();
        int batchSize = 3;

        final BatchProcessor<String> batchProcessor = new BatchProcessor<String>(batchSize)
        {
            @Override
            public void processBatch(User user, List<String> batch)
            {
                output.addAll(batch);
            }
        };

        Batch<String> batch = new Batch<>(null, batchSize, "");
        batch.registerProcessor(batchProcessor);

        for (int i = 0; i < 6; i++) {
            batch.addDataPoint(String.valueOf(i));
        }
        batchProcessor.stopProcessing();

        Assertions.assertIterableEquals(output, ImmutableList.of("0", "1", "2", "3", "4", "5"));
    }
}

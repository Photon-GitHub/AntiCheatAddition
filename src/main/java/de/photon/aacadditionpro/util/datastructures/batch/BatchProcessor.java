package de.photon.aacadditionpro.util.datastructures.batch;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.user.User;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class BatchProcessor<T>
{
    private final int batchSize;
    private ExecutorService executor;

    public BatchProcessor(int batchSize)
    {
        this.batchSize = batchSize;
    }

    public void submit(User user, List<T> batch)
    {
        Preconditions.checkArgument(batch.size() == batchSize, "Batch has wrong size.");

        if (executor != null && !executor.isShutdown()) {
            executor.execute(() -> processBatch(user, batch));
        }
    }

    public abstract void processBatch(User user, List<T> batch);

    public void startProcessing()
    {
        executor = Executors.newCachedThreadPool();
    }

    public void stopProcessing()
    {
        this.executor.shutdown();

        try {
            this.executor.awaitTermination(100, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            // Ignore.
            Thread.currentThread().interrupt();
        }
    }

    public void killProcessing()
    {
        this.executor.shutdownNow();
    }
}

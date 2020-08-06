package de.photon.aacadditionpro.util.datastructures.batch;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.user.User;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public abstract class BatchProcessor<T>
{
    protected final int batchSize;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public void submit(User user, List<T> batch)
    {
        Preconditions.checkArgument(batch.size() == batchSize, "Batch has wrong size.");
        executor.submit(() -> processBatch(user, batch));
    }

    public abstract void processBatch(User user, List<T> batch);

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

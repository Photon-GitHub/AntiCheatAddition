package de.photon.aacadditionpro.util.datastructures.batch;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class BatchProcessor<T>
{
    private final int batchSize;
    private final ExecutorService executor;

    public BatchProcessor(int batchSize)
    {
        this.batchSize = batchSize;
        executor = Executors.newCachedThreadPool();

        for (User user : UserManager.getUsersUnwrapped()) {
            getBatchFromUser(user).registerProcessor(this);
        }
    }

    public void submit(User user, List<T> batch)
    {
        Preconditions.checkArgument(batch.size() == batchSize, "Batch has wrong size.");
        executor.execute(() -> processBatch(user, batch));
    }

    public abstract void processBatch(User user, List<T> batch);

    public abstract Batch<T> getBatchFromUser(User user);

    public void stopProcessing()
    {
        for (User user : UserManager.getUsersUnwrapped()) {
            getBatchFromUser(user).unregisterProcessor(this);
        }

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
        for (User user : UserManager.getUsersUnwrapped()) {
            getBatchFromUser(user).unregisterProcessor(this);
        }

        this.executor.shutdownNow();
    }
}

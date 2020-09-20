package de.photon.aacadditionpro.util.datastructures.batch;

import de.photon.aacadditionpro.user.User;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class AsyncBatchProcessor<T> extends BatchProcessor<T>
{
    private ExecutorService executor;

    public AsyncBatchProcessor(int batchSize)
    {
        super(batchSize);
    }

    @Override
    protected void subSubmit(User user, List<T> batch)
    {
        if (executor != null && !executor.isShutdown()) {
            executor.execute(() -> processBatch(user, batch));
        }
    }

    @Override
    protected void subEnable()
    {
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    protected void subDisable()
    {
        this.executor.shutdownNow();
        this.executor = null;
    }

    public void controlledShutdown()
    {
        this.executor.shutdown();

        try {
            this.executor.awaitTermination(100, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            // Ignore.
            Thread.currentThread().interrupt();
        }
        this.executor = null;
    }
}

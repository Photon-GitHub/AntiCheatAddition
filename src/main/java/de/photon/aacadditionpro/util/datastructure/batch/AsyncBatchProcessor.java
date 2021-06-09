package de.photon.aacadditionpro.util.datastructure.batch;

import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.util.datastructure.broadcast.Broadcaster;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class AsyncBatchProcessor<T> extends BatchProcessor<T>
{
    private ExecutorService executor;

    protected AsyncBatchProcessor(ViolationModule module, Collection<Broadcaster<Batch.Snapshot<T>>> broadcasters)
    {
        super(module, broadcasters);
    }

    @Override
    public void receive(Batch.Snapshot<T> snapshot)
    {
        if (executor != null && !executor.isShutdown()) {
            executor.execute(() -> processBatch(snapshot.getUser(), snapshot.getValues()));
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

    public void controlledShutdown() throws InterruptedException
    {
        this.executor.shutdown();
        //noinspection ResultOfMethodCallIgnored
        this.executor.awaitTermination(100, TimeUnit.DAYS);
        this.executor = null;
    }
}

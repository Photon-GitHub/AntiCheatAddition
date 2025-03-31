package de.photon.anticheataddition.util.datastructure.batch;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import de.photon.anticheataddition.modules.ViolationModule;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract non-sealed class AsyncBatchProcessor<T> extends BatchProcessor<T>
{
    private ExecutorService executor;

    protected AsyncBatchProcessor(ViolationModule module, Set<EventBus> eventBuses)
    {
        super(module, eventBuses);
    }

    @Subscribe
    public final void receive(Batch.Snapshot<T> snapshot)
    {
        if (executor != null && !executor.isShutdown()) {
            executor.execute(() -> processBatch(snapshot.user(), snapshot.values()));
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
        this.executor.awaitTermination(30, TimeUnit.SECONDS);
        this.executor = null;
    }
}

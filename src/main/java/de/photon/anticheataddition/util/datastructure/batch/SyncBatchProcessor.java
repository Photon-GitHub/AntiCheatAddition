package de.photon.anticheataddition.util.datastructure.batch;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import de.photon.anticheataddition.modules.ViolationModule;

import java.util.Set;

public abstract non-sealed class SyncBatchProcessor<T> extends BatchProcessor<T>
{
    protected SyncBatchProcessor(ViolationModule module, Set<EventBus> eventBuses)
    {
        super(module, eventBuses);
    }

    @Subscribe
    public void receive(Batch.Snapshot<T> snapshot)
    {
        this.processBatch(snapshot.user(), snapshot.values());
    }
}

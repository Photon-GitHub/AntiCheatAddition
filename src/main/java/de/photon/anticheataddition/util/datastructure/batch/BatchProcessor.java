package de.photon.anticheataddition.util.datastructure.batch;

import com.google.common.base.Preconditions;
import com.google.common.eventbus.EventBus;
import de.photon.anticheataddition.modules.ConfigLoading;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import lombok.Getter;

import java.util.List;
import java.util.Set;

/**
 * An abstract representation of a processor of {@link Batch.Snapshot}s.
 * <p>
 * The {@link BatchProcessor} will register to {@link EventBus}es to receive the {@link Batch.Snapshot}s and then
 * go on to process them.
 * <p>
 * As the {@link Batch.Snapshot}s can be received asynchronously, thread safety is important.
 */
public abstract sealed class BatchProcessor<T> implements ConfigLoading permits AsyncBatchProcessor, SyncBatchProcessor, VirtualThreadBatchProcessor
{
    @Getter private final ViolationModule module;

    private final Set<EventBus> eventBusses;

    protected BatchProcessor(ViolationModule module, Set<EventBus> eventBuses)
    {
        this.module = module;

        Preconditions.checkNotNull(eventBuses, "Tried to create BatchProcessor with null broadcasters.");
        Preconditions.checkArgument(!eventBuses.isEmpty(), "Tried to create BatchProcessor with empty broadcasters.");
        this.eventBusses = Set.copyOf(eventBuses);
    }

    /**
     * This method represents the actual computation done on the batch.
     */
    public abstract void processBatch(User user, List<T> batch);

    public final void enable()
    {
        for (var eventBus : eventBusses) eventBus.register(this);
        this.subEnable();
    }

    public final void disable()
    {
        for (var eventBus : eventBusses) eventBus.unregister(this);
        this.subDisable();
    }

    protected void subEnable() {}

    protected void subDisable() {}

    @Override
    public String getConfigString()
    {
        return this.module.getConfigString();
    }
}

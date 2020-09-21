package de.photon.aacadditionpro.util.datastructures.batch;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.user.User;

import java.util.List;

public abstract class BatchProcessor<T>
{
    private final int batchSize;
    protected boolean enabled = false;

    public BatchProcessor(int batchSize)
    {
        this.batchSize = batchSize;
    }

    public final void submit(User user, List<T> batch)
    {
        Preconditions.checkArgument(batch.size() == batchSize, "Batch has wrong size.");

        if (this.enabled) {
            subSubmit(user, batch);
        }
    }

    protected abstract void subSubmit(User user, List<T> batch);

    public abstract void processBatch(User user, List<T> batch);

    public final void enable()
    {
        Preconditions.checkState(!enabled, "BatchProcessor is already enabled.");
        this.subEnable();
        this.enabled = true;
    }

    protected abstract void subEnable();

    public final void disable()
    {
        Preconditions.checkState(enabled, "BatchProcessor is already disabled.");
        this.enabled = false;
        this.subDisable();
    }

    protected abstract void subDisable();
}

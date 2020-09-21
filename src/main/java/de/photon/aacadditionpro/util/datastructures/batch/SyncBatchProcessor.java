package de.photon.aacadditionpro.util.datastructures.batch;

import de.photon.aacadditionpro.user.User;

import java.util.List;

public abstract class SyncBatchProcessor<T> extends BatchProcessor<T>
{
    public SyncBatchProcessor(int batchSize)
    {
        super(batchSize);
    }

    @Override
    protected void subSubmit(User user, List<T> batch)
    {
        processBatch(user, batch);
    }

    @Override
    protected void subEnable()
    {
    }

    @Override
    protected void subDisable()
    {
    }
}

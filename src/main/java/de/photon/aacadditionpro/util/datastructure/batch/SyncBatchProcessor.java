package de.photon.aacadditionpro.util.datastructure.batch;

import de.photon.aacadditionpro.util.datastructure.broadcast.Broadcaster;

import java.util.Collection;

public abstract class SyncBatchProcessor<T> extends BatchProcessor<T>
{
    public SyncBatchProcessor(Collection<Broadcaster<Batch.Snapshot<T>>> broadcasters)
    {
        super(broadcasters);
    }

    @Override
    public void receive(Batch.Snapshot<T> snapshot)
    {
        this.processBatch(snapshot.user, snapshot.values);
    }
}

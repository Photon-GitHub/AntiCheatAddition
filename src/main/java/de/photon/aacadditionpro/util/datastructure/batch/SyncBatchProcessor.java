package de.photon.aacadditionpro.util.datastructure.batch;

import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.util.datastructure.broadcast.Broadcaster;

import java.util.Collection;

public abstract class SyncBatchProcessor<T> extends BatchProcessor<T>
{

    protected SyncBatchProcessor(ViolationModule module, Collection<Broadcaster<Batch.Snapshot<T>>> broadcasters)
    {
        super(module, broadcasters);
    }

    @Override
    public void receive(Batch.Snapshot<T> snapshot)
    {
        this.processBatch(snapshot.getUser(), snapshot.getValues());
    }
}

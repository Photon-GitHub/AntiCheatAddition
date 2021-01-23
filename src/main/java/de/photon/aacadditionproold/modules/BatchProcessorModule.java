package de.photon.aacadditionproold.modules;

import de.photon.aacadditionproold.util.datastructures.batch.BatchProcessor;

public interface BatchProcessorModule<T> extends Module
{
    /**
     * Additional chores needed to enable a {@link BatchProcessorModule}
     */
    static <T> void enable(final BatchProcessorModule<T> module)
    {
        module.getBatchProcessor().enable();
    }

    /**
     * Additional chores needed to disable a {@link BatchProcessorModule}
     */
    static <T> void disable(final BatchProcessorModule<T> module)
    {
        module.getBatchProcessor().disable();
    }

    /**
     * Gets the batch processor of this Module.
     */
    BatchProcessor<T> getBatchProcessor();
}

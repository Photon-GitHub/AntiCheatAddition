package de.photon.aacadditionpro.modules;

import de.photon.aacadditionpro.util.datastructures.batch.BatchProcessor;

public interface BatchProcessorModule<T> extends Module
{
    /**
     * Gets the batch processor of this Module.
     */
    BatchProcessor<T> getBatchProcessor();
}

package de.photon.AACAdditionPro.util.datastructures;

import lombok.Getter;

import java.util.DoubleSummaryStatistics;

/**
 * Encapsulates a {@link DoubleSummaryStatistics} to provide a resettable statistics object.
 */
public class DoubleStatistics
{
    @Getter
    private DoubleSummaryStatistics summaryStatistics = new DoubleSummaryStatistics();

    /**
     * Replaces the old {@link DoubleSummaryStatistics} with a new object.
     */
    public void reset()
    {
        this.summaryStatistics = new DoubleSummaryStatistics();
    }
}

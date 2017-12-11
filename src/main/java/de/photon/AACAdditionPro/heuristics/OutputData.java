package de.photon.AACAdditionPro.heuristics;

import lombok.Getter;

public class OutputData extends Data
{
    @Getter
    private double confidence;

    public OutputData(String name)
    {
        super(name);
    }

    public OutputData setConfidence(double confidence)
    {
        this.confidence = confidence;
        return this;
    }
}

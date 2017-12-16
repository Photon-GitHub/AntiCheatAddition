package de.photon.AACAdditionPro.heuristics;

import lombok.Getter;

import java.io.Serializable;

public class OutputData extends Data implements Serializable
{
    public static final OutputData[] DEFAULT_OUTPUT_DATA = new OutputData[]{
            new OutputData("VANILLA"),
            new OutputData("CHEATING")
    };

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

    @Override
    public String toString()
    {
        return "OutputData{" +
               "name=" + this.getName() +
               ",confidence=" + confidence +
               '}';
    }
}

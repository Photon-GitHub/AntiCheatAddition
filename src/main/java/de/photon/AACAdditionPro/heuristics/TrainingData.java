package de.photon.AACAdditionPro.heuristics;

import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

public class TrainingData
{
    @Getter
    private final UUID uuid;
    @Getter
    private final OutputData outputData;

    public int trainingCycles;

    public TrainingData(UUID uuid, OutputData outputData)
    {this(uuid, outputData, 3);}

    public TrainingData(UUID uuid, OutputData outputData, int trainingCycles)
    {
        this.uuid = uuid;
        this.outputData = outputData;
        this.trainingCycles = trainingCycles;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrainingData that = (TrainingData) o;
        return Objects.equals(uuid, that.uuid);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(uuid);
    }
}

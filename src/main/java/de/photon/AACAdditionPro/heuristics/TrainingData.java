package de.photon.AACAdditionPro.heuristics;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

@AllArgsConstructor(suppressConstructorProperties = true)
public class TrainingData
{
    @Getter
    private final UUID uuid;
    @Getter
    private final String outputDataName;

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

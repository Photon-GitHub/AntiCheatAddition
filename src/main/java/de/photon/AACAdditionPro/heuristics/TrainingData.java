package de.photon.AACAdditionPro.heuristics;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@AllArgsConstructor(suppressConstructorProperties = true)
@RequiredArgsConstructor(suppressConstructorProperties = true)
public class TrainingData
{
    @Getter
    private final UUID uuid;
    @Getter
    private final OutputData outputData;

    public int trainingCycles = 3;
}

package de.photon.AACAdditionPro.heuristics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
public class TrainingData
{
    @Getter
    private final UUID uuid;
    @Getter
    private final OutputData outputData;

    public int trainingCycles = 3;
}

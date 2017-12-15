package de.photon.AACAdditionPro.heuristics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

@RequiredArgsConstructor
@Getter
public class TrainingData
{
    private final UUID uuid;
    private final OutputData outputData;
}

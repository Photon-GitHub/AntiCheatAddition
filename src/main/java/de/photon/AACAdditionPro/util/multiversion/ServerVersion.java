package de.photon.AACAdditionPro.util.multiversion;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(suppressConstructorProperties = true)
@Getter
public enum ServerVersion
{
    MC188("1.8.8"),
    MC110("1.10"),
    MC111("1.11"),
    MC112("1.12");

    private final String versionOutputString;
}

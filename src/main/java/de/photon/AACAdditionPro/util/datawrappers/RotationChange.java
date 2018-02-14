package de.photon.AACAdditionPro.util.datawrappers;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class RotationChange
{
    @Getter
    private float yaw;
    @Getter
    private float pitch;
    @Getter
    private final long time = System.currentTimeMillis();

    public void merge(final RotationChange rotationChange)
    {
        this.yaw += rotationChange.yaw;
        this.pitch += rotationChange.pitch;
    }
}

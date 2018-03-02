package de.photon.AACAdditionPro.util.datawrappers;

import de.photon.AACAdditionPro.util.mathematics.RotationUtil;
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

    /**
     * Merges a {@link RotationChange} with this {@link RotationChange}.
     */
    public void merge(final RotationChange rotationChange)
    {
        this.yaw += rotationChange.yaw;
        this.pitch += rotationChange.pitch;
    }

    /**
     * Calculates the total angle between two {@link RotationChange} - directions.
     */
    public float angle(final RotationChange rotationChange)
    {
        return RotationUtil.getDirection(this.getYaw(), this.getPitch()).angle(RotationUtil.getDirection(rotationChange.getYaw(), rotationChange.getPitch()));
    }
}

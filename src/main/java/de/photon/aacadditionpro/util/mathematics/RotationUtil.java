package de.photon.aacadditionpro.util.mathematics;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.util.Vector;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RotationUtil
{

    private static final float FIX_CONVERT_FACTOR = 256.0F / 360.0F;
    private static final float FIX_INVERSE_CONVERT_FACTOR = 360.0F / 256.0F;

    /**
     * Fixes the rotation for the ClientsideLivingEntities
     */
    public static byte getFixRotation(final float yawpitch)
    {
        return (byte) (yawpitch * FIX_CONVERT_FACTOR);
    }

    /**
     * Reconverts rotation values returned by {@link #getFixRotation(float)}
     */
    public static float convertFixedRotation(final byte fixedRotation)
    {
        return fixedRotation * FIX_INVERSE_CONVERT_FACTOR;
    }

    /**
     * Generates the direction - vector from yaw and pitch, basically a copy of {@link Location#getDirection()}
     */
    @SuppressWarnings("RedundantCast")
    public static Vector getDirection(final float yaw, final float pitch)
    {
        val yawRadians = Math.toRadians((double) yaw);
        val pitchRadians = Math.toRadians((double) pitch);

        val vector = new Vector();

        vector.setY(-Math.sin(pitchRadians));

        val xz = Math.cos(pitchRadians);

        vector.setX(-xz * Math.sin(yawRadians));
        vector.setZ(xz * Math.cos(yawRadians));

        return vector;
    }
}

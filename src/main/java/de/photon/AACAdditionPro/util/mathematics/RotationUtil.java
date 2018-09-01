package de.photon.AACAdditionPro.util.mathematics;

import de.photon.AACAdditionPro.util.fakeentity.ClientsideLivingEntity;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class RotationUtil
{
    private static final float FIX_CONVERT_FACTOR = 256.0F / 360.0F;
    private static final float FIX_INVERSE_CONVERT_FACTOR = 360.0F / 256.0F;

    /**
     * This wraps a yaw of any value to a allowed yaw that might be sent in a packet.
     */
    public static float wrapToAllowedYaw(float input)
    {
        return reduceAngleDoubleStep(input, 180);
    }

    /**
     * Reduces the angle to make it fit the spectrum of -minMax til +minMax in steps of two times minMax.
     * Used for certain yaw calculations.
     *
     * @param input  the initial angle
     * @param minMax the boundary in the positive and negative spectrum. The parameter itself must be > 0.
     */
    public static float reduceAngleDoubleStep(float input, float minMax)
    {
        final float doubleMinMax = 2 * minMax;

        input = input % doubleMinMax;

        return reduceAngle(input, minMax, doubleMinMax);
    }

    /**
     * Reduces the angle to make it fit the spectrum of -minMax til +minMax in steps of minMax
     *
     * @param input  the initial angle
     * @param minMax the boundary in the positive and negative spectrum. The parameter itself must be > 0.
     */
    public static float reduceAngle(float input, float minMax)
    {
        final float doubleMinMax = 2 * minMax;

        input = input % doubleMinMax;

        return reduceAngle(input, minMax, minMax);
    }

    /**
     * Util method to prevent code duplicates.
     * This adds or subtracts step if input is not in range of -minMax <= input <= minMax
     */
    private static float reduceAngle(float input, float minMax, float step)
    {
        if (input >= minMax)
        {
            input -= step;
        }
        else if (input < -minMax)
        {
            input += step;
        }

        return input;
    }

    /**
     * Fixes the rotation for the {@link ClientsideLivingEntity}s
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
    public static Vector getDirection(final float yaw, final float pitch)
    {
        Vector vector = new Vector();

        vector.setY(-Math.sin(Math.toRadians((double) pitch)));

        double xz = Math.cos(Math.toRadians((double) pitch));

        vector.setX(-xz * Math.sin(Math.toRadians((double) yaw)));
        vector.setZ(xz * Math.cos(Math.toRadians((double) yaw)));

        return vector;
    }
}

package de.photon.AACAdditionPro.util.mathematics;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class RotationUtil
{
    /**
     * Reduces the angle to make it fit the spectrum of -minMax til +minMax in steps of minMax
     *
     * @param input  the initial angle
     * @param minMax the boundary in the positive and negative spectrum. The parameter itself must be > 0.
     */
    //TODO: CHECK IF THE ANGLE REDUCING SHOULD REALLY WORK THIS WAY.
    public static float reduceAngle(float input, float minMax)
    {
        final float doubleMinMax = 2 * minMax;

        input = input % doubleMinMax;

        if (input >= minMax)
        {
            input -= minMax;
        }

        if (input < -minMax)
        {
            input += minMax;
        }

        return input;
    }

    /**
     * Fixes the rotation for the {@link de.photon.AACAdditionPro.util.fakeentity.ClientsideEntity}s
     */
    public static byte getFixRotation(final float yawpitch)
    {
        return (byte) ((int) (yawpitch * 256.0F / 360.0F));
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

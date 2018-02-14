package de.photon.AACAdditionPro.util.mathematics;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public final class RotationUtil
{
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

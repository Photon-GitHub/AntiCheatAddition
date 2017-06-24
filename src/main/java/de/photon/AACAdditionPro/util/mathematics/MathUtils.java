package de.photon.AACAdditionPro.util.mathematics;


import org.bukkit.Location;

public final class MathUtils
{
    /**
     * Simple method to know if a number is close to another number
     *
     * @param a     The first number
     * @param b     The second number
     * @param range The maximum search range
     *
     * @return true if the numbers are in range of one another else false
     */
    public static boolean isInRange(final double a, final double b, final double range)
    {
        if (a == b) {
            return true;
        }

        if (a > b) {
            return (a - range) <= b;
        } else {
            return (b - range) <= a;
        }
    }

    /**
     * Simple method to know if a {@link Location} is close to another {@link Location}
     *
     * @param firstLocation  the first {@link Location}
     * @param secondLocation the second {@link Location}
     * @param x              how far should the {@link Location}s at most be apart on the x-Axis
     * @param y              how far should the {@link Location}s at most be apart on the y-Axis
     * @param z              how far should the {@link Location}s at most be apart on the z-Axis
     *
     * @return true if the {@link Location} are in range, false if not
     */
    public static boolean areLocationsInRange(final Location firstLocation, final Location secondLocation, final double x, final double y, final double z)
    {
        return firstLocation.getWorld().getName().equals(secondLocation.getWorld().getName()) &&
               isInRange(firstLocation.getX(), secondLocation.getX(), x) &&
               isInRange(firstLocation.getY(), secondLocation.getY(), y) &&
               isInRange(firstLocation.getZ(), secondLocation.getZ(), z);
    }

    /**
     * Simple method to know if a {@link Location} is close to another {@link Location}
     *
     * @param firstLocation  the first {@link Location}
     * @param secondLocation the second {@link Location}
     * @param sqaredDistance the squared distance that must be at most between the two {@link Location}s to make this {@link java.lang.reflect.Method} return true.
     *
     * @return true if the {@link Location} are in range, false if not
     */
    public static boolean areLocationsInRange(final Location firstLocation, final Location secondLocation, final double sqaredDistance)
    {
        return firstLocation.getWorld().getName().equals(secondLocation.getWorld().getName()) &&
               firstLocation.distanceSquared(secondLocation) <= sqaredDistance;
    }

    /**
     * Fixes the rotation for the {@link de.photon.AACAdditionPro.util.clientsideentities.ClientsideEntity}s
     */
    public static byte getFixRotation(final float yawpitch)
    {
        return (byte) ((int) (yawpitch * 256.0F / 360.0F));
    }
}

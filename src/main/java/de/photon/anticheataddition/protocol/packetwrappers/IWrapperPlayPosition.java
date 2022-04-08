package de.photon.anticheataddition.protocol.packetwrappers;

import de.photon.anticheataddition.ServerVersion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public interface IWrapperPlayPosition extends IWrapperPlay
{
    private double getCoordinate(int oldMCFieldIndex, int fieldIndex)
    {
        return ServerVersion.is18() ?
               getHandle().getIntegers().read(oldMCFieldIndex) / 32.0 :
               getHandle().getDoubles().read(fieldIndex);
    }

    private void setCoordinate(int oldMCFieldIndex, int fieldIndex, double value)
    {
        if (ServerVersion.is18()) getHandle().getIntegers().write(oldMCFieldIndex, (int) (value * 32));
        else getHandle().getDoubles().write(fieldIndex, value);
    }

    /**
     * Retrieve X.
     * <p>
     * Notes: absolute position
     *
     * @return The current X
     */
    default double getX()
    {
        return getCoordinate(1, 0);
    }

    /**
     * Set X.
     *
     * @param value - new value.
     */
    default void setX(final double value)
    {
        setCoordinate(1, 0, value);
    }

    /**
     * Retrieve Feet Y.
     * <p>
     * Notes: absolute feet position. Is normally HeadY - 1.62. Used to modify
     * the players bounding box when going upstairs, crouching, etc…
     *
     * @return The current FeetY
     */
    default double getY()
    {
        return getCoordinate(2, 1);
    }

    /**
     * Set Feet Y.
     *
     * @param value - new value.
     */
    default void setY(final double value)
    {
        setCoordinate(2, 1, value);
    }

    /**
     * Retrieve Z.
     * <p>
     * Notes: absolute position
     *
     * @return The current Z
     */
    default double getZ()
    {
        return getCoordinate(3, 2);
    }

    /**
     * Set Z.
     *
     * @param value - new value.
     */
    default void setZ(final double value)
    {
        setCoordinate(3, 2, value);
    }

    /**
     * Retrieve the position as a vector.
     *
     * @return The position as a vector.
     */
    default Vector toVector()
    {
        return new Vector(this.getX(), this.getY(), this.getZ());
    }

    default Location toLocation(World world)
    {
        return new Location(world, this.getX(), this.getY(), this.getZ());
    }

    default void setWithLocation(Location location)
    {
        this.setX(location.getX());
        this.setY(location.getY());
        this.setZ(location.getZ());
    }
}

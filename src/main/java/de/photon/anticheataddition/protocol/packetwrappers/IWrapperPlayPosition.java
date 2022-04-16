package de.photon.anticheataddition.protocol.packetwrappers;

import com.comphenix.protocol.events.PacketContainer;
import de.photon.anticheataddition.ServerVersion;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public interface IWrapperPlayPosition extends IWrapperPlay
{
    static void setIntDoublePosition(PacketContainer handle, int fieldIndex, double value)
    {
        // The oldMCFieldIndex has always been modern fieldIndex + 1 so far.
        if (ServerVersion.is18()) handle.getIntegers().write(fieldIndex + 1, (int) (value * 32));
        else handle.getDoubles().write(fieldIndex, value);
    }

    static double getIntDoublePosition(PacketContainer handle, int fieldIndex)
    {
        return ServerVersion.is18() ?
               // The oldMCFieldIndex has always been modern fieldIndex + 1 so far.
               handle.getIntegers().read(fieldIndex + 1) / 32.0D :
               handle.getDoubles().read(fieldIndex);
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
        return getHandle().getDoubles().read(0);
    }

    /**
     * Set X.
     *
     * @param value - new value.
     */
    default void setX(final double value)
    {
        getHandle().getDoubles().write(0, value);
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
        return getHandle().getDoubles().read(1);
    }

    /**
     * Set Feet Y.
     *
     * @param value - new value.
     */
    default void setY(final double value)
    {
        getHandle().getDoubles().write(1, value);
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
        return getHandle().getDoubles().read(2);
    }

    /**
     * Set Z.
     *
     * @param value - new value.
     */
    default void setZ(final double value)
    {
        getHandle().getDoubles().write(2, value);
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

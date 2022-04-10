package de.photon.anticheataddition.protocol.packetwrappers.sentbyserver;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.protocol.packetwrappers.IWrapperPlayOnGround;
import org.bukkit.util.Vector;

public interface IWrapperPlayServerRelEntityMove extends IWrapperPlayOnGround
{
    private double getCoordinate(int oldMCFieldIndex, int fieldIndex)
    {
        return ServerVersion.is18() ?
               getHandle().getBytes().read(oldMCFieldIndex) / 32D :
               // Integers are ok, even though wiki.vg says short
               getHandle().getIntegers().read(fieldIndex) / 4096D;
    }

    private void setCoordinate(int oldMCFieldIndex, int fieldIndex, double value)
    {
        Preconditions.checkArgument(value <= 8, "Tried to move relative " + value + " blocks when teleport is needed.");

        if (ServerVersion.is18()) getHandle().getBytes().write(oldMCFieldIndex, (byte) (value * 32));
            // Integers are ok, even though wiki.vg says short
        else getHandle().getIntegers().write(fieldIndex, (int) (value * 4096));
    }

    /**
     * Get the x difference.
     */
    default double getDx()
    {
        return getCoordinate(0, 1);
    }

    /**
     * Set the x difference.
     */
    default void setDx(double value)
    {
        setCoordinate(0, 1, value);
    }

    /**
     * Get the y difference.
     */
    default double getDy()
    {
        return getCoordinate(1, 2);
    }

    /**
     * Set the y difference.
     */
    default void setDy(double value)
    {
        setCoordinate(1, 2, value);
    }

    /**
     * Get the z difference.
     */
    default double getDz()
    {
        return getCoordinate(2, 3);
    }

    /**
     * Set the z difference.
     */
    default void setDz(double value)
    {
        setCoordinate(2, 3, value);
    }

    /**
     * Set all differences with a {@link Vector}
     *
     * @param vector the {@link Vector} which x- y- and z- coordinates will be used to set this {@link IWrapperPlayServerRelEntityMove}
     */
    default void setDiffs(final Vector vector)
    {
        this.setDiffs(vector.getX(), vector.getY(), vector.getZ());
    }

    /**
     * Set all differences at once.
     * This is a shortcut method for calling {@link #setDx(double)}, {@link #setDy(double)} and {@link #setDz(double)} in a sequence.
     *
     * @param xDiff the new x difference
     * @param yDiff the new y difference
     * @param zDiff the new z difference
     */
    default void setDiffs(double xDiff, double yDiff, double zDiff)
    {
        setDx(xDiff);
        setDy(yDiff);
        setDz(zDiff);
    }
}

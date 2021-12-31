package de.photon.aacadditionpro.protocol.packetwrappers.sentbyserver;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.exception.UnknownMinecraftException;
import de.photon.aacadditionpro.protocol.packetwrappers.IWrapperPlayOnGround;
import org.bukkit.util.Vector;

public interface IWrapperPlayServerRelEntityMove extends IWrapperPlayOnGround
{
    /**
     * Get the x difference.
     */
    default double getDx()
    {
        return ServerVersion.is18() ?
               getHandle().getBytes().read(0) / 32D :
               // Integers are ok, even though wiki.vg says short
               getHandle().getIntegers().read(1) / 4096D;
    }

    /**
     * Set the x difference.
     */
    default void setDx(double value)
    {
        Preconditions.checkArgument(value <= 8, "Tried to move relative x: " + value + " blocks when teleport is needed.");

        if (ServerVersion.is18()) getHandle().getBytes().write(0, (byte) (value * 32));
            // Integers are ok, even though wiki.vg says short
        else getHandle().getIntegers().write(1, (int) (value * 4096));
    }

    /**
     * Get the y difference.
     */
    default double getDy()
    {
        return ServerVersion.is18() ?
               getHandle().getBytes().read(1) / 32D :
               // Integers are ok, even though wiki.vg says short
               getHandle().getIntegers().read(2) / 4096D;
    }

    /**
     * Set the y difference.
     */
    default void setDy(double value)
    {
        Preconditions.checkArgument(value <= 8, "Tried to move relative y: " + value + " blocks when teleport is needed.");

        if (ServerVersion.is18()) getHandle().getBytes().write(1, (byte) (value * 32));
            // Integers are ok, even though wiki.vg says short
        else getHandle().getIntegers().write(2, (int) (value * 4096));
    }

    /**
     * Get the z difference.
     */
    default double getDz()
    {
        return ServerVersion.is18() ?
               getHandle().getBytes().read(2) / 32D :
               // Integers are ok, even though wiki.vg says short
               getHandle().getIntegers().read(3) / 4096D;
    }

    /**
     * Set the z difference.
     */
    default void setDz(double value)
    {
        Preconditions.checkArgument(value <= 8, "Tried to move relative z: " + value + " blocks when teleport is needed.");

        if (ServerVersion.is18()) getHandle().getBytes().write(1, (byte) (value * 32));
            // Integers are ok, even though wiki.vg says short
        else getHandle().getIntegers().write(2, (int) (value * 4096));

        switch (ServerVersion.getActiveServerVersion()) {
            case MC18:
                getHandle().getBytes().write(2, (byte) (value * 32));
                break;
            case MC112:
            case MC115:
            case MC116:
            case MC117:
            case MC118:
                // Integers are ok, even though wiki.vg says short
                getHandle().getIntegers().write(3, (int) (value * 4096));
                break;
            default:
                throw new UnknownMinecraftException();
        }
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

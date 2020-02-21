package de.photon.aacadditionpro.util.packetwrappers.server;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.util.exceptions.UnknownMinecraftVersion;
import de.photon.aacadditionpro.util.packetwrappers.IWrapperPlayOnGround;
import org.bukkit.util.Vector;

public interface IWrapperPlayServerRelEntityMove extends IWrapperPlayOnGround
{
    /**
     * Get the x difference.
     */
    default double getDx()
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                return getHandle().getBytes().read(0) / 32D;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
                // Integers are ok, even though wiki.vg says short
                return getHandle().getIntegers().read(1) / 4096D;
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    /**
     * Set the x difference.
     */
    default void setDx(double value)
    {
        // Preconditions.checkArgument(value <= 8, "Tried to move relative " + value + " blocks when teleport is needed.");
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                getHandle().getBytes().write(0, (byte) (value * 32));
                break;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
                // Integers are ok, even though wiki.vg says short
                getHandle().getIntegers().write(1, (int) (value * 4096));
                break;
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    /**
     * Get the y difference.
     */
    default double getDy()
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                return getHandle().getBytes().read(1) / 32D;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
                // Integers are ok, even though wiki.vg says short
                return getHandle().getIntegers().read(2) / 4096D;
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    /**
     * Set the y difference.
     */
    default void setDy(double value)
    {
        // Preconditions.checkArgument(value <= 8, "Tried to move relative " + value + " blocks when teleport is needed.");
        Preconditions.checkArgument(value <= 8, "Tried to move relative " + value + " blocks when teleport is needed.");
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                getHandle().getBytes().write(1, (byte) (value * 32));
                break;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
                // Integers are ok, even though wiki.vg says short
                getHandle().getIntegers().write(2, (int) (value * 4096));
                break;
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    /**
     * Get the z difference.
     */
    default double getDz()
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                return getHandle().getBytes().read(2) / 32D;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
                // Integers are ok, even though wiki.vg says short
                return getHandle().getIntegers().read(3) / 4096D;
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    /**
     * Set the z difference.
     */
    default void setDz(double value)
    {
        // Preconditions.checkArgument(value <= 8, "Tried to move relative " + value + " blocks when teleport is needed.");
        Preconditions.checkArgument(value <= 8, "Tried to move relative " + value + " blocks when teleport is needed.");
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
                getHandle().getBytes().write(2, (byte) (value * 32));
                break;
            case MC112:
            case MC113:
            case MC114:
            case MC115:
                // Integers are ok, even though wiki.vg says short
                getHandle().getIntegers().write(3, (int) (value * 4096));
                break;
            default:
                throw new UnknownMinecraftVersion();
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

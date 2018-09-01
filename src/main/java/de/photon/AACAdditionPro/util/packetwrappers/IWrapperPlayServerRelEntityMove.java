package de.photon.AACAdditionPro.util.packetwrappers;

import org.bukkit.util.Vector;

public interface IWrapperPlayServerRelEntityMove extends IWrapperPlayClientOnGround
{
    /**
     * Get the x difference.
     */
    double getDx();

    /**
     * Set the x difference.
     */
    void setDx(double value);

    /**
     * Get the y difference.
     */
    double getDy();

    /**
     * Set the y difference.
     */
    void setDy(double value);

    /**
     * Get the z difference.
     */
    double getDz();

    /**
     * Set the z difference.
     */
    void setDz(double value);

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

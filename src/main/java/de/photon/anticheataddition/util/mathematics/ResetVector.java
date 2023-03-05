package de.photon.anticheataddition.util.mathematics;

import org.bukkit.util.Vector;

public final class ResetVector extends Vector
{
    private final double resetX;
    private final double resetY;
    private final double resetZ;

    public ResetVector(Vector baseVector)
    {
        this.resetX = baseVector.getX();
        this.resetY = baseVector.getY();
        this.resetZ = baseVector.getZ();
        resetToBase();
    }

    public ResetVector(double resetX, double resetY, double resetZ)
    {
        this.resetX = resetX;
        this.resetY = resetY;
        this.resetZ = resetZ;
        resetToBase();
    }

    public ResetVector resetToBase()
    {
        this.x = this.resetX;
        this.y = this.resetY;
        this.z = this.resetZ;
        return this;
    }
}
package de.photon.aacadditionpro.util.mathematics;

import lombok.EqualsAndHashCode;
import org.bukkit.util.Vector;

// Do not call super, as we only want to check the base vector.
@EqualsAndHashCode(callSuper = false)
public class ResetVector extends Vector
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
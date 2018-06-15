package de.photon.AACAdditionPro.util.mathematics;

import org.bukkit.util.Vector;

/**
 * This class represents a pyramid with a rectangular base.
 */
public class PyramidRectangle implements Cloneable
{
    protected Vector peak;
    protected Vector[] basis = new Vector[4];
    protected Vector[] normalizedEdges = new Vector[4];

    public PyramidRectangle(Vector peak, Vector first, Vector second, Vector third, Vector fourth)
    {
        this.peak = peak;
        basis[0] = first;
        basis[1] = second;
        basis[2] = third;
        basis[3] = fourth;

        for (int i = 0; i < basis.length; i++)
        {
            normalizedEdges[i] = basis[i].clone().subtract(peak).normalize();
        }
    }
}

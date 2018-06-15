package de.photon.AACAdditionPro.util.mathematics;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import java.util.List;

/**
 * Represents a pyramid which base is parallel to one plane.
 */
public class ParallelBasePyramidRectangle extends PyramidRectangle
{
    // Calculate the iteration vector
    private BlockFace iterationFace;
    private double iterationMin;
    private double iterationMax;

    public ParallelBasePyramidRectangle(Vector peak, Vector first, Vector second, Vector third, Vector fourth)
    {
        super(peak, first, second, third, fourth);
        final Vector orthogonalVector = this.basis[1].clone().subtract(this.basis[0]).crossProduct(this.basis[3].clone().subtract(this.basis[0]));

        if (orthogonalVector.getX() != 0)
        {
            iterationMin = this.peak.getX();
            iterationMax = this.basis[0].getX() - iterationMin;
            iterationFace = Math.signum(iterationMax) > 0 ? BlockFace.EAST : BlockFace.WEST;
        }
        else if (orthogonalVector.getY() != 0)
        {
            iterationMin = this.peak.getY();
            iterationMax = this.basis[0].getY() - iterationMin;
            iterationFace = Math.signum(iterationMax) > 0 ? BlockFace.UP : BlockFace.DOWN;
        }
        else if (orthogonalVector.getZ() != 0)
        {
            iterationMin = this.peak.getZ();
            iterationMax = this.basis[0].getZ() - iterationMin;
            iterationFace = Math.signum(iterationMax) > 0 ? BlockFace.SOUTH : BlockFace.NORTH;
        }

        iterationMax = Math.abs(iterationMax);
    }

    /**
     * Gets all the {@link Block}s inside this pyramid.
     */
    public List<Block> getBlocksInside(World world)
    {
        Vector current = this.peak;

        /*
        In order to get the length of a pyramid side
         */
        for (double d = this.iterationMin; d < iterationMax; d++)
        {

            // Add the iteration coordinates
            current.setX(current.getX() + iterationFace.getModX());
            current.setX(current.getY() + iterationFace.getModY());
            current.setX(current.getZ() + iterationFace.getModZ());
        }
        //TODO: Complete method.
        return null;
    }

    public AxisAlignedBB constructBasisRectangle()
    {
        return new AxisAlignedBB(this.basis[0].getX(), this.basis[0].getY(), this.basis[0].getZ(),
                                 this.basis[2].getX(), this.basis[2].getY(), this.basis[2].getZ());
    }
}

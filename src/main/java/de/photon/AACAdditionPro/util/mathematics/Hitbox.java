package de.photon.AACAdditionPro.util.mathematics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Getter
public enum Hitbox
{
    PLAYER(0.3D, 0.3D, 1.8D),
    SNEAKING_PLAYER(0.3D, 0.3D, 1.65D);

    private final double offsetX;
    private final double offsetZ;
    private final double height;

    /**
     * This gets a {@link List} of Vectors, which is especially helpful for raytracing
     *
     * @param location         the initial {@link Location} of the {@link org.bukkit.entity.Entity}, thus the basis of the {@link Hitbox}.
     * @param addCenterVectors whether only the {@link Vector}s of the corners should be returned in the {@link List} or additional {@link Vector}s
     *                         in the center of the {@link org.bukkit.entity.Entity} (alongside the y-axis) should be added
     *
     * @return a {@link List} of all the constructed {@link Vector}s.
     */
    public List<Vector> getCalculationVectors(final Location location, final boolean addCenterVectors)
    {
        final List<Vector> vectors = new ArrayList<>(addCenterVectors ?
                                                     11 :
                                                     8);
        final Vector start = location.toVector();

        //Lower corners
        vectors.add(new Vector(start.getX() + this.offsetX, start.getY(), start.getZ() + this.offsetZ));
        vectors.add(new Vector(start.getX() - this.offsetX, start.getY(), start.getZ() + this.offsetZ));
        vectors.add(new Vector(start.getX() + this.offsetX, start.getY(), start.getZ() - this.offsetZ));
        vectors.add(new Vector(start.getX() - this.offsetX, start.getY(), start.getZ() - this.offsetZ));

        //Upper corners
        vectors.add(new Vector(start.getX() + this.offsetX, start.getY() + this.height, start.getZ() + this.offsetZ));
        vectors.add(new Vector(start.getX() - this.offsetX, start.getY() + this.height, start.getZ() + this.offsetZ));
        vectors.add(new Vector(start.getX() + this.offsetX, start.getY() + this.height, start.getZ() - this.offsetZ));
        vectors.add(new Vector(start.getX() - this.offsetX, start.getY() + this.height, start.getZ() - this.offsetZ));

        if (addCenterVectors)
        {
            // Steps and other blocks with irregular hitboxes need more steps (below 0.5 blocks)
            final double step_size = (location.getY() - location.getBlockX() > 0.1) ? 0.47D : 1D;

            for (double d = 1.47; d < this.height - 1; d += step_size)
            {
                vectors.add(new Vector(start.getX(), start.getY() + d, start.getZ()));
            }
        }
        return vectors;
    }

    /**
     * Constructs an {@link AxisAlignedBB} on the basis of the provided {@link Location}
     *
     * @param location the {@link Location} to base the bounding box on.
     */
    public AxisAlignedBB constructBoundingBox(final Location location)
    {
        return new AxisAlignedBB(
                location.getX() - this.offsetX,
                // The location is based on the feet location
                location.getY(),
                location.getZ() - this.offsetZ,

                location.getX() + this.offsetX,
                // The location is based on the feet location
                location.getY() + this.height,
                location.getZ() + this.offsetZ
        );
    }
}

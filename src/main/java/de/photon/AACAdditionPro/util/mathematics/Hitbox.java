package de.photon.AACAdditionPro.util.mathematics;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.List;

@RequiredArgsConstructor
@Getter
public enum Hitbox
{
    /**
     * The normal hitbox of a player
     */
    PLAYER(0.3D, 0.3D, 1.8D),
    /**
     * The hitbox of a sneaking player
     */
    SNEAKING_PLAYER(0.3D, 0.3D, 1.65D),
    /**
     * A hitbox that covers the whole body (which is partially outside the normal hitbox)
     */
    ESP_PLAYER(0.5, 0.5, 1.8D),
    /**
     * A hitbox that covers the whole body (which is partially outside the normal hitbox)while the player is sneaking
     */
    ESP_SNEAKING_PLAYER(0.5, 0.5, 1.65D);

    private final double offsetX;
    private final double offsetZ;
    private final double height;

    /**
     * Creates an array of {@link Vector}s that indicates essential positions of the hitbox which are helpful for
     * raytracing.
     *
     * @param location         the initial {@link Location} of the {@link org.bukkit.entity.Entity}, thus the basis of the {@link Hitbox}.
     * @param addCenterVectors whether only the {@link Vector}s of the corners should be returned in the {@link List} or additional {@link Vector}s
     *                         in the center of the {@link org.bukkit.entity.Entity} (alongside the y-axis) should be added
     *
     * @return an array of all the constructed {@link Vector}s.
     */
    public Vector[] getCalculationVectors(final Location location, final boolean addCenterVectors)
    {
        // 9 because the +0 y - vector in the beginning is not calculated by the division.
        final Vector[] vectors = new Vector[addCenterVectors ? (int) (9 + (this.height / 0.47)) : 8];
        byte currentIndex = 0;

        final Vector start = location.toVector();
        final double lowerY = start.getY();
        final double upperY = lowerY + this.height;

        //Lower corners
        vectors[currentIndex++] = new Vector(start.getX() + this.offsetX, lowerY, start.getZ() + this.offsetZ);
        vectors[currentIndex++] = new Vector(start.getX() - this.offsetX, lowerY, start.getZ() + this.offsetZ);
        vectors[currentIndex++] = new Vector(start.getX() + this.offsetX, lowerY, start.getZ() - this.offsetZ);
        vectors[currentIndex++] = new Vector(start.getX() - this.offsetX, lowerY, start.getZ() - this.offsetZ);

        //Upper corners
        vectors[currentIndex++] = new Vector(start.getX() + this.offsetX, upperY, start.getZ() + this.offsetZ);
        vectors[currentIndex++] = new Vector(start.getX() - this.offsetX, upperY, start.getZ() + this.offsetZ);
        vectors[currentIndex++] = new Vector(start.getX() + this.offsetX, upperY, start.getZ() - this.offsetZ);
        vectors[currentIndex++] = new Vector(start.getX() - this.offsetX, upperY, start.getZ() - this.offsetZ);

        if (addCenterVectors)
        {
            start.setX(start.getX() + this.offsetX / 2);
            start.setX(start.getZ() + this.offsetZ / 2);

            // 0.47 as a factor as of slabs and other irregular block models.
            for (double d = 0; d < this.height; d += 0.47)
            {
                vectors[currentIndex++] = start.clone().setY(lowerY + d);
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

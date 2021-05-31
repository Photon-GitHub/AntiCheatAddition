package de.photon.aacadditionpro.util.mathematics;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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

    public Vector[] getLowResolutionCalculationVectors(final Location location)
    {

        val lowerY = location.getY();
        val upperY = lowerY + this.height;

        val cullX = (location.getBlockX() - (int) (location.getX() + offsetX)) == 0 && (location.getBlockX() - (int) (location.getX() - offsetX)) == 0;
        val cullZ = (location.getBlockZ() - (int) (location.getZ() + offsetZ)) == 0 && (location.getBlockZ() - (int) (location.getZ() - offsetZ)) == 0;

        if (cullX && cullZ) {
            val vectors = new Vector[2];
            vectors[0] = (new Vector(location.getX(), lowerY, location.getZ()));
            vectors[1] = (new Vector(location.getX() + this.offsetX, upperY, location.getZ() + this.offsetZ));
            return vectors;
        } else if (cullX) {
            val vectors = new Vector[4];
            vectors[0] = (new Vector(location.getX(), lowerY, location.getZ() + this.offsetZ));
            vectors[1] = (new Vector(location.getX(), lowerY, location.getZ() - this.offsetZ));
            vectors[2] = (new Vector(location.getX(), upperY, location.getZ() + this.offsetZ));
            vectors[3] = (new Vector(location.getX(), upperY, location.getZ() - this.offsetZ));
            return vectors;
        } else if (cullZ) {
            val vectors = new Vector[4];
            vectors[0] = (new Vector(location.getX() + this.offsetX, lowerY, location.getZ()));
            vectors[1] = (new Vector(location.getX() - this.offsetX, lowerY, location.getZ()));
            vectors[2] = (new Vector(location.getX() + this.offsetX, upperY, location.getZ()));
            vectors[3] = (new Vector(location.getX() - this.offsetX, upperY, location.getZ()));
            return vectors;
        } else {
            val vectors = new Vector[8];

            // Lower corners
            vectors[0] = (new Vector(location.getX() + this.offsetX, lowerY, location.getZ() + this.offsetZ));
            vectors[1] = (new Vector(location.getX() - this.offsetX, lowerY, location.getZ() + this.offsetZ));
            vectors[2] = (new Vector(location.getX() + this.offsetX, lowerY, location.getZ() - this.offsetZ));
            vectors[3] = (new Vector(location.getX() - this.offsetX, lowerY, location.getZ() - this.offsetZ));

            // Upper corners
            vectors[4] = (new Vector(location.getX() + this.offsetX, upperY, location.getZ() + this.offsetZ));
            vectors[5] = (new Vector(location.getX() - this.offsetX, upperY, location.getZ() + this.offsetZ));
            vectors[6] = (new Vector(location.getX() + this.offsetX, upperY, location.getZ() - this.offsetZ));
            vectors[7] = (new Vector(location.getX() - this.offsetX, upperY, location.getZ() - this.offsetZ));
            return vectors;
        }
    }

    /**
     * Creates an array of {@link Vector}s that indicates essential positions of the hitbox which are helpful for
     * raytracing.
     *
     * @param location the initial {@link Location} of the {@link org.bukkit.entity.Entity}, thus the basis of the {@link Hitbox}.
     *
     * @return an array of all the constructed {@link Vector}s.
     */
    public Vector[] getCalculationVectors(final Location location)
    {
        val vectors = new ArrayList<Vector>(13);
        Collections.addAll(vectors, getLowResolutionCalculationVectors(location));

        val upperY = location.getY() + this.height;

        Vector start = location.toVector();
        while (start.getY() < upperY) {
            vectors.add(start);
            // 0.47 as a factor as of slabs and other irregular block models.
            start = start.clone().setY(start.getY() + 0.47);
        }
        return vectors.toArray(new Vector[0]);
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

    public int[] getPartiallyIncludedBlocksCoordinates(@NotNull final Location location)
    {
        val coordinates = new int[6];
        coordinates[0] = (int) (location.getX() - this.offsetX);
        coordinates[1] = (int) location.getY();
        coordinates[2] = (int) (location.getZ() - this.offsetZ);

        // Add 1 to ceil the value as the cast to int floors it.
        coordinates[3] = (int) (location.getX() + this.offsetX + 1);
        coordinates[4] = (int) (location.getY() + this.height + 1);
        coordinates[5] = (int) (location.getZ() + this.offsetZ + 1);
        return coordinates;
    }

    /**
     * Gets all the {@link Block}s that this {@link Hitbox} is partially inside.
     */
    public List<Block> getPartiallyIncludedBlocks(@NotNull final Location location)
    {
        Preconditions.checkNotNull(location.getWorld(), "Tried to get blocks in hitbox of location with null world.");

        val c = getPartiallyIncludedBlocksCoordinates(location);
        final List<Block> blocks = new ArrayList<>(MathUtil.absDiff(c[0], c[3]) * MathUtil.absDiff(c[1], c[4]) * MathUtil.absDiff(c[2], c[5]) + 1);

        for (; c[0] <= c[3]; ++c[0]) {
            for (; c[1] <= c[4]; ++c[1]) {
                for (; c[2] <= c[5]; ++c[2]) {
                    blocks.add(location.getWorld().getBlockAt(c[0], c[1], c[2]));
                }
            }
        }
        return blocks;
    }

    /**
     * Gets all the {@link Material}s that this {@link Hitbox} is partially inside.
     */
    public Set<Material> getPartiallyIncludedMaterials(@NotNull final Location location)
    {
        Preconditions.checkNotNull(location.getWorld(), "Tried to get blocks in hitbox of location with null world.");

        val c = getPartiallyIncludedBlocksCoordinates(location);
        val materials = EnumSet.noneOf(Material.class);

        for (; c[0] <= c[3]; ++c[0]) {
            for (; c[1] <= c[4]; ++c[1]) {
                for (; c[2] <= c[5]; ++c[2]) {
                    materials.add(location.getWorld().getBlockAt(c[0], c[1], c[2]).getType());
                }
            }
        }
        return materials;
    }
}

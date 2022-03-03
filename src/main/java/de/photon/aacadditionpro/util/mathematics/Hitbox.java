package de.photon.aacadditionpro.util.mathematics;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.util.minecraft.world.MaterialUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

    public static Hitbox fromPlayer(Player player)
    {
        return player.isSneaking() ? SNEAKING_PLAYER : PLAYER;
    }

    public Location[] getEspLocations(final Location location)
    {
        val cullX = (location.getBlockX() - (int) (location.getX() + offsetX)) == 0 && (location.getBlockX() - (int) (location.getX() - offsetX)) == 0;
        val cullZ = (location.getBlockZ() - (int) (location.getZ() + offsetZ)) == 0 && (location.getBlockZ() - (int) (location.getZ() - offsetZ)) == 0;

        val world = location.getWorld();

        val x = location.getX();

        val lowerY = location.getY();
        val upperY = lowerY + this.height;

        val z = location.getZ();

        if (cullX && cullZ) return new Location[]{
                new Location(world, x, lowerY, z),
                new Location(world, x, upperY, z)
        };
        else if (cullX) return new Location[]{
                new Location(world, x, lowerY, z + this.offsetZ),
                new Location(world, x, lowerY, z - this.offsetZ),
                new Location(world, x, upperY, z + this.offsetZ),
                new Location(world, x, upperY, z - this.offsetZ)
        };
        else if (cullZ) return new Location[]{
                new Location(world, x + this.offsetX, lowerY, z),
                new Location(world, x - this.offsetX, lowerY, z),
                new Location(world, x + this.offsetX, upperY, z),
                new Location(world, x - this.offsetX, upperY, z)
        };

        return new Location[]{
                // Lower corners
                new Location(world, x + this.offsetX, lowerY, z + this.offsetZ),
                new Location(world, x - this.offsetX, lowerY, z + this.offsetZ),
                new Location(world, x + this.offsetX, lowerY, z - this.offsetZ),
                new Location(world, x - this.offsetX, lowerY, z - this.offsetZ),

                // Upper corners
                new Location(world, x + this.offsetX, upperY, z + this.offsetZ),
                new Location(world, x - this.offsetX, upperY, z + this.offsetZ),
                new Location(world, x + this.offsetX, upperY, z - this.offsetZ),
                new Location(world, x - this.offsetX, upperY, z - this.offsetZ)
        };
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

    /**
     * Checks whether any {@link Block}s that are partially inside this {@link Hitbox} are liquids as defined in {@link MaterialUtil#LIQUIDS}
     */
    public boolean isInLiquids(@NotNull final Location location)
    {
        return MaterialUtil.containsLiquids(this.getPartiallyIncludedMaterials(location));
    }
}

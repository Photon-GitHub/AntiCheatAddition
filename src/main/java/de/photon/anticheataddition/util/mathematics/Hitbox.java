package de.photon.anticheataddition.util.mathematics;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.util.datastructure.SetUtil;
import de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
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

    public static HitboxLocation espHitboxLocationOf(Player player)
    {
        return new HitboxLocation(player.isSneaking() ? ESP_SNEAKING_PLAYER : ESP_PLAYER, player.getLocation());
    }

    public static HitboxLocation hitboxLocationOf(Player player)
    {
        return new HitboxLocation(player.isSneaking() ? SNEAKING_PLAYER : PLAYER, player.getLocation());
    }

    public record HitboxLocation(@NotNull Hitbox hitbox, @NotNull Location location) implements Iterable<Location>
    {
        public Location[] getEspLocations()
        {
            final boolean cullX = (location.getBlockX() - (int) (location.getX() + hitbox.offsetX)) == 0 && (location.getBlockX() - (int) (location.getX() - hitbox.offsetX)) == 0;
            final boolean cullZ = (location.getBlockZ() - (int) (location.getZ() + hitbox.offsetZ)) == 0 && (location.getBlockZ() - (int) (location.getZ() - hitbox.offsetZ)) == 0;
            final World world = location.getWorld();

            final double x = location.getX();

            final double lowerY = location.getY();
            final double upperY = lowerY + hitbox.height;

            final double z = location.getZ();

            if (cullX && cullZ) return new Location[]{
                    new Location(world, x, lowerY, z),
                    new Location(world, x, upperY, z)
            };
            else if (cullX) return new Location[]{
                    new Location(world, x, lowerY, z + hitbox.offsetZ),
                    new Location(world, x, lowerY, z - hitbox.offsetZ),
                    new Location(world, x, upperY, z + hitbox.offsetZ),
                    new Location(world, x, upperY, z - hitbox.offsetZ)
            };
            else if (cullZ) return new Location[]{
                    new Location(world, x + hitbox.offsetX, lowerY, z),
                    new Location(world, x - hitbox.offsetX, lowerY, z),
                    new Location(world, x + hitbox.offsetX, upperY, z),
                    new Location(world, x - hitbox.offsetX, upperY, z)
            };

            return new Location[]{
                    // Lower corners
                    new Location(world, x + hitbox.offsetX, lowerY, z + hitbox.offsetZ),
                    new Location(world, x - hitbox.offsetX, lowerY, z + hitbox.offsetZ),
                    new Location(world, x + hitbox.offsetX, lowerY, z - hitbox.offsetZ),
                    new Location(world, x - hitbox.offsetX, lowerY, z - hitbox.offsetZ),

                    // Upper corners
                    new Location(world, x + hitbox.offsetX, upperY, z + hitbox.offsetZ),
                    new Location(world, x - hitbox.offsetX, upperY, z + hitbox.offsetZ),
                    new Location(world, x + hitbox.offsetX, upperY, z - hitbox.offsetZ),
                    new Location(world, x - hitbox.offsetX, upperY, z - hitbox.offsetZ)
            };
        }

        /**
         * Constructs an {@link AxisAlignedBB} based on this {@link HitboxLocation}.
         */
        public AxisAlignedBB createBoundingBox()
        {
            return new AxisAlignedBB(
                    location.getX() - this.hitbox.offsetX, location.getY(), location.getZ() - this.hitbox.offsetZ,
                    location.getX() + this.hitbox.offsetX, location.getY() + this.hitbox.height, location.getZ() + this.hitbox.offsetZ
            );
        }

        /**
         * Checks whether any {@link Block}s that are partially inside this {@link HitboxLocation} are liquids as defined in {@link MaterialUtil#getLiquids()}
         */
        public boolean isInLiquids()
        {
            return SetUtil.containsAny(MaterialUtil.INSTANCE.getLiquids(), getPartiallyIncludedMaterials());
        }

        /**
         * Gets all the {@link Material}s that this {@link HitboxLocation} is partially inside.
         */
        public Set<Material> getPartiallyIncludedMaterials()
        {
            return getPartiallyIncludedBlocks().stream().map(Block::getType).collect(SetUtil.toImmutableEnumSet());
        }

        /**
         * Gets all the {@link Block}s that this {@link HitboxLocation} is partially inside.
         */
        public List<Block> getPartiallyIncludedBlocks()
        {
            final var world = Preconditions.checkNotNull(location.getWorld(), "Tried to get blocks from null world.");
            return getPartiallyIncludedLocations().stream().map(world::getBlockAt).toList();
        }

        public List<Location> getPartiallyIncludedLocations()
        {
            final var locations = new ArrayList<Location>();
            final var world = location.getWorld();

            // Iterate over all the blocks inside the hitbox.
            // Note: <= is used as min and max can be the same block, which should be included.
            for (int x = (int) (location.getX() - hitbox.offsetX), maxX = (int) (location.getX() + hitbox.offsetX); x <= maxX; ++x) {
                for (int y = (int) location.getY(), maxY = (int) (location.getY() + hitbox.height); y <= maxY; ++y) {
                    for (int z = (int) (location.getZ() - hitbox.offsetZ), maxZ = (int) (location.getZ() + hitbox.offsetZ); z <= maxZ; ++z) {
                        locations.add(new Location(world, x, y, z));
                    }
                }
            }
            return locations;
        }

        @NotNull
        @Override
        public Iterator<Location> iterator()
        {
            return getPartiallyIncludedLocations().iterator();
        }
    }
}

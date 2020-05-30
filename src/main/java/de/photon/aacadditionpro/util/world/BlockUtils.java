package de.photon.aacadditionpro.util.world;

import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.util.exceptions.UnknownMinecraftVersion;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public final class BlockUtils
{
    public static final Set<Material> LIQUIDS;
    /**
     * Contains all containers that need a free space of any kind above the container (e.g. chests with a stair above)
     */
    public static final Set<Material> FREE_SPACE_CONTAINERS;
    public static final Set<Material> FREE_SPACE_CONTAINERS_ALLOWED_MATERIALS;
    public static final Set<BlockFace> HORIZONTAL_FACES = Collections.unmodifiableSet(EnumSet.of(
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.EAST));
    public static final Set<BlockFace> ALL_FACES = Collections.unmodifiableSet(EnumSet.of(
            BlockFace.UP,
            BlockFace.DOWN,
            BlockFace.NORTH,
            BlockFace.SOUTH,
            BlockFace.WEST,
            BlockFace.EAST));

    static {
        final EnumSet<Material> freeSpaceMaterials = EnumSet.of(Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST);
        final EnumSet<Material> allowedMaterials = EnumSet.of(Material.AIR, Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST, Material.ANVIL);

        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC112:
                for (Material material : Material.values()) {
                    if (material.name().endsWith("_SLAB") ||
                        material.name().endsWith("_STAIRS"))
                    {
                        allowedMaterials.add(material);
                    }

                    if (material.name().endsWith("SHULKER_BOX")) {
                        freeSpaceMaterials.add(material);
                    }
                }

                allowedMaterials.add(Material.getMaterial("ENCHANTMENT_TABLE"));

                LIQUIDS = Collections.unmodifiableSet(EnumSet.of(Material.WATER,
                                                                 Material.LAVA,
                                                                 Material.getMaterial("STATIONARY_WATER"),
                                                                 Material.getMaterial("STATIONARY_LAVA")));
                break;
            case MC113:
                for (Material material : Material.values()) {
                    if (material.name().endsWith("_SLAB") ||
                        material.name().endsWith("_STAIRS"))
                    {
                        allowedMaterials.add(material);
                    }

                    if (material.name().endsWith("SHULKER_BOX")) {
                        freeSpaceMaterials.add(material);
                    }
                }

                allowedMaterials.add(Material.CAVE_AIR);
                // The enchantment table was renamed as well.
                allowedMaterials.add(Material.ENCHANTING_TABLE);

                LIQUIDS = Collections.unmodifiableSet(EnumSet.of(Material.WATER,
                                                                 Material.LAVA));
                break;
            case MC114:
            case MC115:
                for (Material material : Material.values()) {
                    if (material.name().endsWith("_SLAB") ||
                        material.name().endsWith("_SIGN") ||
                        material.name().endsWith("_STAIRS"))
                    {
                        allowedMaterials.add(material);
                    }

                    if (material.name().endsWith("SHULKER_BOX")) {
                        freeSpaceMaterials.add(material);
                    }
                }

                allowedMaterials.add(Material.CAVE_AIR);
                // The enchantment table was renamed as well.
                allowedMaterials.add(Material.ENCHANTING_TABLE);

                LIQUIDS = Collections.unmodifiableSet(EnumSet.of(Material.WATER,
                                                                 Material.LAVA));
                break;
            default:
                throw new UnknownMinecraftVersion();
        }

        FREE_SPACE_CONTAINERS = Collections.unmodifiableSet(freeSpaceMaterials);
        FREE_SPACE_CONTAINERS_ALLOWED_MATERIALS = Collections.unmodifiableSet(allowedMaterials);
    }

    private BlockUtils() {}

    /**
     * This can be used to know if the {@link Block}s are next to each other.
     *
     * @param a              the first {@link Block}
     * @param b              the second {@link Block}
     * @param onlyHorizontal whether only the horizontal {@link BlockFace}s should be checked for the second {@link Block} or all {@link BlockFace}s (horizontal + vertical)
     *
     * @return true if the {@link Block}s are next to each other, or false if they are not
     */
    public static boolean isNext(final Block a, final Block b, final boolean onlyHorizontal)
    {
        // Same world
        return a.getWorld().getUID().equals(b.getWorld().getUID())
               // Next to each other.
               && (onlyHorizontal ?
                   HORIZONTAL_FACES.contains(a.getFace(b)) :
                   ALL_FACES.contains(a.getFace(b))
               );
    }

    /**
     * This counts the {@link Block}s around the given block if they are not air/empty.
     *
     * @param block          the block that faces should be checked for other {@link Block}s
     * @param onlyHorizontal whether only the {@link Block}s should be counted that are horizontal around the block or all {@link Block}s (horizontal + vertical)
     *
     * @return the amount of {@link Block}s which were counted
     */
    public static byte countBlocksAround(final Block block, final boolean onlyHorizontal)
    {
        byte count = 0;
        for (final BlockFace f : onlyHorizontal ?
                                 HORIZONTAL_FACES :
                                 ALL_FACES) {
            if (!block.getRelative(f).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * This gets the {@link Block}s around the given block if they are not air/empty.
     *
     * @param block          the block that faces should be checked for other {@link Block}s
     * @param onlyHorizontal whether only the {@link Block}s should be counted that are horizontal around the block or all {@link Block}s (horizontal + vertical)
     *
     * @return a {@link List} of all {@link Block}s which were found.
     */
    public static List<Block> getBlocksAround(final Block block, final boolean onlyHorizontal)
    {
        final List<Block> blocks = new ArrayList<>(onlyHorizontal ? 4 : 6);
        for (final BlockFace face : onlyHorizontal ? HORIZONTAL_FACES : ALL_FACES) {
            final Block relative = block.getRelative(face);
            if (!relative.isEmpty()) {
                blocks.add(relative);
            }
        }
        return blocks;
    }

    /**
     * Fix for Spigot's broken occluding method.
     */
    public static boolean isReallyOccluding(Material material)
    {
        switch (ServerVersion.getActiveServerVersion()) {
            case MC188:
            case MC112:
                return material != Material.BARRIER && material != Material.getMaterial("MOB_SPAWNER") && material.isOccluding();
            case MC113:
            case MC114:
            case MC115:
                return material != Material.BARRIER && material != Material.SPAWNER && material.isOccluding();
            default:
                throw new UnknownMinecraftVersion();
        }
    }

    /**
     * This determines if an {@link org.bukkit.inventory.InventoryView} can be opened by interacting with the {@link Block}.
     */
    public static boolean isInventoryOpenable(@NotNull final Block block)
    {
        // The block actually is holding an inventory (and therefore is not e.g. dirt)
        if (block.getState() instanceof InventoryHolder) {
            // Additional checks for cats and occluding blocks necessary?
            if (FREE_SPACE_CONTAINERS.contains(block.getType())) {
                final Block aboveBlock = block.getRelative(BlockFace.UP);

                switch (ServerVersion.getActiveServerVersion()) {
                    case MC188:
                    case MC112:
                        // 1.8.8 and 1.12 doesn't provide isPassable.
                        // Make sure that the block above is not obstructed by blocks
                        return FREE_SPACE_CONTAINERS_ALLOWED_MATERIALS.contains(aboveBlock.getType());
                    // Cannot check for cats on 1.8 and 1.12 as the server version doesn't provide the newer methods.
                    case MC113:
                        // Make sure that the block above is not obstructed by blocks
                        if (!(FREE_SPACE_CONTAINERS_ALLOWED_MATERIALS.contains(aboveBlock.getType()) ||
                              aboveBlock.isPassable()
                        ))
                        {
                            return false;
                        }

                        // Make sure that the block above is not obstructed by cats
                        return aboveBlock.getWorld().getNearbyEntities(aboveBlock.getLocation(), 0.5, 1, 0.5, entity -> entity.getType() == EntityType.OCELOT).isEmpty();
                    case MC114:
                    case MC115:
                        // Make sure that the block above is not obstructed by blocks
                        if (!(FREE_SPACE_CONTAINERS_ALLOWED_MATERIALS.contains(aboveBlock.getType()) ||
                              aboveBlock.isPassable()
                        ))
                        {
                            return false;
                        }

                        // Make sure that the block above is not obstructed by cats
                        return aboveBlock.getWorld().getNearbyEntities(aboveBlock.getLocation(), 0.5, 1, 0.5, entity -> entity.getType() == EntityType.CAT).isEmpty();
                    default:
                        throw new UnknownMinecraftVersion();
                }
            }
            return true;
        }
        return false;
    }
}

package de.photon.aacadditionpro.util.minecraft.world;

import com.google.common.collect.Sets;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.exception.UnknownMinecraftException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MaterialUtil
{
    // A set of materials which hitboxes changed in minecraft 1.9
    public static final Set<Material> CHANGED_HITBOX_MATERIALS = ServerVersion.is18() ? Sets.immutableEnumSet(Material.getMaterial("STAINED_GLASS_PANE"),
                                                                                                              Material.getMaterial("THIN_GLASS"),
                                                                                                              Material.getMaterial("IRON_FENCE"),
                                                                                                              Material.CHEST,
                                                                                                              Material.ANVIL) : Set.of();

    public static final Material EXPERIENCE_BOTTLE;

    public static final Set<Material> LIQUIDS;

    /**
     * Contains all containers that need a free space of any kind above the container (e.g. chests with a stair above)
     */
    public static final Set<Material> FREE_SPACE_CONTAINERS;
    public static final Set<Material> FREE_SPACE_CONTAINERS_ALLOWED_MATERIALS;

    static {
        val freeSpaceContainers = EnumSet.of(Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST);
        val allowedMaterials = EnumSet.of(Material.AIR, Material.CHEST, Material.TRAPPED_CHEST, Material.ENDER_CHEST, Material.ANVIL);

        switch (ServerVersion.getActiveServerVersion()) {
            case MC18:
            case MC112:
                allowedMaterials.addAll(getMaterialsEndingWith("_SLAB", "_STAIRS", "SIGN"));

                freeSpaceContainers.addAll(getMaterialsEndingWith("SHULKER_BOK"));
                allowedMaterials.add(Material.getMaterial("ENCHANTMENT_TABLE"));

                EXPERIENCE_BOTTLE = Material.getMaterial("EXP_BOTTLE");
                LIQUIDS = Sets.immutableEnumSet(Material.WATER, Material.LAVA, Material.getMaterial("STATIONARY_WATER"), Material.getMaterial("STATIONARY_LAVA"));
                break;
            case MC115:
            case MC116:
            case MC117:
            case MC118:
                allowedMaterials.addAll(ofTags(Tag.STANDING_SIGNS, Tag.WALL_SIGNS, Tag.SIGNS,
                                               Tag.SLABS,
                                               Tag.STAIRS, Tag.WOODEN_STAIRS));

                freeSpaceContainers.addAll(ofTags(Tag.SHULKER_BOXES));

                allowedMaterials.add(Material.CAVE_AIR);
                allowedMaterials.add(Material.ENCHANTING_TABLE);

                EXPERIENCE_BOTTLE = Material.EXPERIENCE_BOTTLE;
                LIQUIDS = Sets.immutableEnumSet(Material.WATER, Material.LAVA);
                break;
            default:
                throw new UnknownMinecraftException();
        }

        FREE_SPACE_CONTAINERS = Sets.immutableEnumSet(freeSpaceContainers);
        FREE_SPACE_CONTAINERS_ALLOWED_MATERIALS = Sets.immutableEnumSet(allowedMaterials);
    }

    public static Set<Material> getMaterialsEndingWith(String... ends)
    {
        val materials = EnumSet.noneOf(Material.class);
        for (Material material : Material.values()) {
            if (StringUtils.endsWithAny(material.name(), ends)) materials.add(material);
        }
        return materials;
    }

    @SafeVarargs
    public static Set<Material> ofTags(Tag<Material>... tags)
    {
        return Sets.immutableEnumSet(Arrays.stream(tags)
                                           .flatMap(tag -> tag.getValues().stream())
                                           .collect(Collectors.toUnmodifiableSet()));
    }

    /**
     * Fix for Spigot's broken occluding method.
     */
    public static boolean isReallyOccluding(Material material)
    {
        return material != Material.BARRIER && material.isOccluding()
               // Pre-1.13 versions have a different spawner material.
               && ServerVersion.getActiveServerVersion().compareTo(ServerVersion.MC113) < 0 ? material != Material.getMaterial("MOB_SPAWNER") : material != Material.SPAWNER;
    }

    /**
     * Checks if a {@link Collection} of {@link Material}s contains any of certain {@link Material}s.
     */
    public static boolean containsMaterials(@NotNull final Collection<Material> searchFor, @NotNull final Collection<Material> toBeSearched)
    {
        for (Material material : searchFor) if (toBeSearched.contains(material)) return true;
        return false;
    }

    /**
     * Checks if a {@link Collection} of {@link Material}s contains liquids.
     */
    public static boolean containsLiquids(@NotNull final Collection<Material> toBeSearched)
    {
        return containsMaterials(LIQUIDS, toBeSearched);
    }
}

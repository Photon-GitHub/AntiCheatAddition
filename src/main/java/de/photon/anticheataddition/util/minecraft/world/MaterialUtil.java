package de.photon.anticheataddition.util.minecraft.world;

import com.google.common.collect.Sets;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.exception.UnknownMinecraftException;
import de.photon.anticheataddition.util.datastructure.SetUtil;
import lombok.experimental.UtilityClass;
import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import static org.bukkit.Material.AIR;
import static org.bukkit.Material.ANVIL;
import static org.bukkit.Material.CAVE_AIR;
import static org.bukkit.Material.CHEST;
import static org.bukkit.Material.ENDER_CHEST;
import static org.bukkit.Material.LAVA;
import static org.bukkit.Material.SLIME_BLOCK;
import static org.bukkit.Material.TRAPPED_CHEST;
import static org.bukkit.Material.VOID_AIR;
import static org.bukkit.Material.WATER;
import static org.bukkit.Material.getMaterial;
import static org.bukkit.Material.values;

@UtilityClass
public final class MaterialUtil
{

    /**
     * Materials which bounce the player up when jumping or landing on them.
     */
    public static final Set<Material> BOUNCE_MATERIALS;
    /**
     * Materials which can cause an automatic step upwards (e.g. slabs and stairs)
     */
    public static final Set<Material> AUTO_STEP_MATERIALS;
    public static final Material EXPERIENCE_BOTTLE;
    public static final Set<Material> SIGNS;
    public static final Material SPAWNER;
    public static final Set<Material> LIQUIDS;
    /**
     * Contains all containers that need a free space of any kind above the container (e.g. chests with a stair above)
     */
    public static final Set<Material> FREE_SPACE_CONTAINERS;
    // A set of materials which hitboxes changed in minecraft 1.9
    public static final Set<Material> CHANGED_HITBOX_MATERIALS = ServerVersion.is18() ? Sets.immutableEnumSet(ANVIL,
                                                                                                              CHEST,
                                                                                                              getMaterial("STAINED_GLASS_PANE"),
                                                                                                              getMaterial("THIN_GLASS"),
                                                                                                              getMaterial("IRON_FENCE")) : Set.of();

    private static final Set<Material> AIR_MATERIALS = ServerVersion.MC116.activeIsLaterOrEqual() ?
                                                       Sets.immutableEnumSet(AIR, CAVE_AIR, VOID_AIR) :
                                                       Sets.immutableEnumSet(AIR);

    static {
        final var autoStepMaterials = EnumSet.of(CHEST, TRAPPED_CHEST, ENDER_CHEST);
        final var bounceMaterials = EnumSet.of(SLIME_BLOCK);
        final var freeSpaceContainers = EnumSet.of(CHEST, TRAPPED_CHEST, ENDER_CHEST);

        switch (ServerVersion.ACTIVE) {
            case MC18, MC112 -> {
                autoStepMaterials.addAll(getMaterialsEndingWith("_STAIRS", "_SLABS"));
                // This will automatically exclude the "BED" on 1.8.8, as bed bouncing was introduced in 1.12.
                bounceMaterials.addAll(getMaterialsEndingWith("_BED"));
                freeSpaceContainers.addAll(getMaterialsEndingWith("SHULKER_BOK"));
                EXPERIENCE_BOTTLE = getMaterial("EXP_BOTTLE");
                SIGNS = getMaterialsEndingWith("SIGN");
                SPAWNER = getMaterial("MOB_SPAWNER");
                LIQUIDS = Sets.immutableEnumSet(WATER, LAVA, getMaterial("STATIONARY_WATER"), getMaterial("STATIONARY_LAVA"));
            }

            case MC115, MC116, MC117, MC118, MC119 -> {
                autoStepMaterials.addAll(ofTags(Tag.SLABS, Tag.WOODEN_SLABS, Tag.STAIRS, Tag.WOODEN_STAIRS));
                bounceMaterials.addAll(ofTags(Tag.BEDS));
                freeSpaceContainers.addAll(ofTags(Tag.SHULKER_BOXES));
                EXPERIENCE_BOTTLE = Material.EXPERIENCE_BOTTLE;
                SIGNS = ofTags(Tag.SIGNS, Tag.STANDING_SIGNS, Tag.WALL_SIGNS);
                SPAWNER = Material.SPAWNER;
                LIQUIDS = Sets.immutableEnumSet(WATER, LAVA);
            }

            default -> throw new UnknownMinecraftException();
        }

        AUTO_STEP_MATERIALS = Sets.immutableEnumSet(autoStepMaterials);
        BOUNCE_MATERIALS = Sets.immutableEnumSet(bounceMaterials);
        FREE_SPACE_CONTAINERS = Sets.immutableEnumSet(freeSpaceContainers);
    }

    public static Set<Material> getMaterialsEndingWith(String... ends)
    {
        return Arrays.stream(values())
                     .filter(material -> Arrays.stream(ends).anyMatch(material.name()::endsWith))
                     .collect(SetUtil.toImmutableEnumSet());
    }

    @SafeVarargs
    public static Set<Material> ofTags(Tag<Material>... tags)
    {
        return Arrays.stream(tags)
                     .map(Tag::getValues)
                     .flatMap(Set::stream)
                     .collect(SetUtil.toImmutableEnumSet());
    }

    /**
     * Fix for Spigot's broken occluding method.
     */
    public static boolean isReallyOccluding(Material material)
    {
        return switch (material) {
            case BARRIER, SPAWNER -> false;
            default -> material.isOccluding();
        };
    }

    public static boolean isAir(Material material)
    {
        return AIR_MATERIALS.contains(material);
    }
}

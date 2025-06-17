package de.photon.anticheataddition.util.minecraft.world.material;

import com.google.common.collect.Sets;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.datastructure.SetUtil;
import lombok.Getter;
import org.bukkit.Material;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

import static de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil.combineToImmutable;
import static de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil.getMaterialsEndingWith;
import static org.bukkit.Material.*;


@Getter
final class AncientMaterialUtil implements MaterialUtil
{
    private final Set<Material> airMaterials = Sets.immutableEnumSet(AIR);
    private final Set<Material> autoStepMaterials = combineToImmutable(EnumSet.of(CHEST, TRAPPED_CHEST, ENDER_CHEST), getMaterialsEndingWith("_STAIRS", "_SLABS", "_STEP"));

    // This will automatically exclude the "BED" on 1.8.8, as bed bouncing was introduced in 1.12.
    private final Set<Material> bounceMaterials = combineToImmutable(EnumSet.of(SLIME_BLOCK), getMaterialsEndingWith("_BED"));

    private final Set<Material> changedHitboxMaterials = ServerVersion.is18() ? Sets.immutableEnumSet(ANVIL,
                                                                                                      CHEST,
                                                                                                      getMaterial("STAINED_GLASS_PANE"),
                                                                                                      getMaterial("THIN_GLASS"),
                                                                                                      getMaterial("IRON_FENCE")) : Set.of();

    private final Set<Material> freeSpaceContainers = combineToImmutable(EnumSet.of(CHEST, TRAPPED_CHEST, ENDER_CHEST), getMaterialsEndingWith("SHULKER_BOK"));
    private final Set<Material> nonOpenableInventories = Set.of();
    private final Set<Material> liquids = Sets.immutableEnumSet(WATER, LAVA, getMaterial("STATIONARY_WATER"), getMaterial("STATIONARY_LAVA"));
    private final Set<Material> signs = getMaterialsEndingWith("SIGN");

    // Tools
    private final Set<Material> axes = getMaterialsEndingWith("_AXE");
    private final Set<Material> hoes = getMaterialsEndingWith("_HOE");
    private final Set<Material> pickaxes = getMaterialsEndingWith("_PICKAXE");
    private final Set<Material> shovels = getMaterialsEndingWith("_SHOVEL");
    private final Set<Material> swords = getMaterialsEndingWith("_SWORD");

    // Mined by tools
    private final Set<Material> minedByAxes = Arrays.stream(values()).filter(material -> material.name().contains("WOOD") || material.name().endsWith("_LOG") || material.name().contains("PLANKS")
                                                                                         || material.name().contains("BAMBOO") || material.name().contains("CHEST") || material.name().equals("BARREL")
                                                                                         || material.name().contains("BOOKSHELF") || material.name().equals("LADDER")
                                                                                         || material.name().contains("SIGN") || material.name().contains("CAMPFIRE")
                                                                                         || material.name().equals("NOTE_BLOCK") || material.name().endsWith("_TABLE")).collect(SetUtil.toImmutableEnumSet());

    private final Set<Material> minedByHoes = Arrays.stream(values()).filter(material -> material.name().contains("HAY") || material.name().contains("CROP") || material.name().contains("WART")
                                                                                         || material.name().contains("LEAVES") || material.name().contains("MOSS") || material.name().equals("DRIED_KELP_BLOCK")
                                                                                         || material.name().equals("TARGET")).collect(SetUtil.toImmutableEnumSet());

    private final Set<Material> minedByPickaxes = Arrays.stream(values()).filter(material -> (material.name().contains("STONE") || material.name().contains("DEEPSLATE") || material.name().contains("ORE")
                                                                                              || material.name().contains("TERRACOTTA") || material.name().endsWith("_BLOCK")
                                                                                              || material.name().equals("OBSIDIAN") || material.name().equals("CRYING_OBSIDIAN")
                                                                                              || material.name().equals("NETHERRACK") || material.name().equals("END_STONE")
                                                                                              || (material.name().startsWith("RAW_") && material.name().endsWith("_BLOCK"))
                                                                                              || material.name().equals("ANCIENT_DEBRIS"))).collect(SetUtil.toImmutableEnumSet());

    private final Set<Material> minedByShovels = Arrays.stream(values()).filter(material -> (material.name().contains("DIRT") || material.name().contains("GRAVEL") || material.name().contains("SAND")
                                                                                             || material.name().contains("SNOW") || material.name().contains("MUD") || material.name().contains("CLAY")
                                                                                             || material.name().equals("GRASS_BLOCK") || material.name().equals("PODZOL") || material.name().equals("ROOTED_DIRT")
                                                                                             || material.name().endsWith("CONCRETE_POWDER") || material.name().equals("SOUL_SAND") || material.name().equals("SOUL_SOIL"))).collect(SetUtil.toImmutableEnumSet());

    private final Set<Material> minedBySwords = Arrays.stream(values()).filter(material -> material.name().equals("BAMBOO") || material.name().equals("BAMBOO_SHOOT")).collect(SetUtil.toImmutableEnumSet());

    private final Material expBottle = getMaterial("EXP_BOTTLE");
    private final Material spawner = getMaterial("MOB_SPAWNER");
}

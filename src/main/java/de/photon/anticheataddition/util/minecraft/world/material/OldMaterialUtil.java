package de.photon.anticheataddition.util.minecraft.world.material;

import com.google.common.collect.Sets;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.EnumSet;
import java.util.Set;

import static de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil.combineToImmutable;
import static de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil.ofTags;
import static org.bukkit.Material.*;

@Getter
final class OldMaterialUtil implements MaterialUtil
{
    private final Set<Material> airMaterials = Sets.immutableEnumSet(AIR, CAVE_AIR, VOID_AIR);

    private final Set<Material> autoStepMaterials = combineToImmutable(EnumSet.of(CHEST, TRAPPED_CHEST, ENDER_CHEST), ofTags(Tag.SLABS, Tag.STAIRS));
    private final Set<Material> bounceMaterials = combineToImmutable(EnumSet.of(SLIME_BLOCK), ofTags(Tag.BEDS));
    private final Set<Material> freeSpaceContainers = combineToImmutable(EnumSet.of(CHEST, TRAPPED_CHEST, ENDER_CHEST), ofTags(Tag.SHULKER_BOXES));
    private final Set<Material> nonOpenableInventories = Set.of();

    private final Set<Material> liquids = Sets.immutableEnumSet(WATER, LAVA);
    private final Set<Material> signs = ofTags(Tag.ALL_SIGNS);

    // Tools
    private final Set<Material> axes = Sets.immutableEnumSet(WOODEN_AXE, GOLDEN_AXE, STONE_AXE, IRON_AXE, DIAMOND_AXE, NETHERITE_AXE);
    private final Set<Material> hoes = Sets.immutableEnumSet(WOODEN_HOE, GOLDEN_HOE, STONE_HOE, IRON_HOE, DIAMOND_HOE, NETHERITE_HOE);
    private final Set<Material> pickaxes = Sets.immutableEnumSet(WOODEN_PICKAXE, GOLDEN_PICKAXE, STONE_PICKAXE, IRON_PICKAXE, DIAMOND_PICKAXE, NETHERITE_PICKAXE);
    private final Set<Material> shovels = Sets.immutableEnumSet(WOODEN_SHOVEL, GOLDEN_SHOVEL, STONE_SHOVEL, IRON_SHOVEL, DIAMOND_SHOVEL, NETHERITE_SHOVEL);
    private final Set<Material> swords = Sets.immutableEnumSet(WOODEN_SWORD, GOLDEN_SWORD, STONE_SWORD, IRON_SWORD, DIAMOND_SWORD, NETHERITE_SWORD);

    // Mined by tools
    private final Set<Material> minedByAxes = ofTags(Tag.MINEABLE_AXE);

    private final Set<Material> minedByHoes = ofTags(Tag.MINEABLE_HOE);

    private final Set<Material> minedByPickaxes = ofTags(Tag.MINEABLE_PICKAXE);

    private final Set<Material> minedByShovels = ofTags(Tag.MINEABLE_SHOVEL);

    private final Set<Material> minedBySwords = Sets.immutableEnumSet(BAMBOO, BAMBOO_SAPLING);


    private final Material expBottle = getMaterial("EXP_BOTTLE");
    private final Material spawner = getMaterial("MOB_SPAWNER");
}

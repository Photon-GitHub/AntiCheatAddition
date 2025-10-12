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
final class ModernMaterialUtil implements MaterialUtil {
    private final Set<Material> airMaterials = Sets.immutableEnumSet(AIR, CAVE_AIR, VOID_AIR);

    private final Set<Material> autoStepMaterials = combineToImmutable(EnumSet.of(CHEST, TRAPPED_CHEST, ENDER_CHEST), ofTags(Tag.SLABS, Tag.STAIRS, Tag.COPPER_CHESTS));
    private final Set<Material> bounceMaterials = combineToImmutable(EnumSet.of(SLIME_BLOCK), ofTags(Tag.BEDS));
    private final Set<Material> freeSpaceContainers = combineToImmutable(EnumSet.of(CHEST, TRAPPED_CHEST, ENDER_CHEST), ofTags(Tag.SHULKER_BOXES, Tag.COPPER_CHESTS));
    private final Set<Material> nonOpenableInventories = combineToImmutable(EnumSet.of(CHISELED_BOOKSHELF, DECORATED_POT), ofTags(Tag.WOODEN_SHELVES));

    // Tools
    private final Set<Material> axes = ofTags(Tag.ITEMS_AXES);
    private final Set<Material> hoes = ofTags(Tag.ITEMS_HOES);
    private final Set<Material> pickaxes = ofTags(Tag.ITEMS_PICKAXES);
    private final Set<Material> shovels = ofTags(Tag.ITEMS_SHOVELS);
    private final Set<Material> swords = ofTags(Tag.ITEMS_SWORDS);

    // Mined by tools
    private final Set<Material> minedByAxes = ofTags(Tag.MINEABLE_AXE);

    private final Set<Material> minedByHoes = ofTags(Tag.MINEABLE_HOE);

    private final Set<Material> minedByPickaxes = ofTags(Tag.MINEABLE_PICKAXE);

    private final Set<Material> minedByShovels = ofTags(Tag.MINEABLE_SHOVEL);

    private final Set<Material> minedBySwords = ofTags(Tag.SWORD_EFFICIENT, Tag.SWORD_INSTANTLY_MINES);

    private final Set<Material> liquids = Sets.immutableEnumSet(WATER, LAVA);
    private final Set<Material> signs = ofTags(Tag.ALL_SIGNS);
}

package de.photon.anticheataddition.util.minecraft.world.material;

import com.google.common.collect.Sets;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil.combineToImmutable;
import static de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil.ofTags;
import static org.bukkit.Material.*;

@Getter
class ModernMaterialUtil implements MaterialUtil
{
    private final Set<Material> airMaterials = Sets.immutableEnumSet(AIR, CAVE_AIR, VOID_AIR);

    private final Set<Material> autoStepMaterials = combineToImmutable(EnumSet.of(CHEST, TRAPPED_CHEST, ENDER_CHEST), ofTags(Tag.SLABS, Tag.STAIRS));
    private final Set<Material> bounceMaterials = combineToImmutable(EnumSet.of(SLIME_BLOCK), ofTags(Tag.BEDS));
    private final Set<Material> freeSpaceContainers = combineToImmutable(EnumSet.of(CHEST, TRAPPED_CHEST, ENDER_CHEST), ofTags(Tag.SHULKER_BOXES));

    private final Set<Material> liquids = Sets.immutableEnumSet(WATER, LAVA);
    private final Set<Material> signs = ofTags(Tag.ALL_SIGNS);

    @Override
    public Optional<Material> getChiseledBookshelf()
    {
        return Optional.of(CHISELED_BOOKSHELF);
    }
}

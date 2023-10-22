package de.photon.anticheataddition.util.minecraft.world.material;

import com.google.common.collect.Sets;
import de.photon.anticheataddition.ServerVersion;
import lombok.Getter;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

import static de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil.combineToImmutable;
import static de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil.getMaterialsEndingWith;
import static org.bukkit.Material.*;


@Getter
class AncientMaterialUtil implements MaterialUtil
{
    private final Set<Material> airMaterials = Sets.immutableEnumSet(AIR);
    private final Set<Material> autoStepMaterials = combineToImmutable(EnumSet.of(CHEST, TRAPPED_CHEST, ENDER_CHEST), getMaterialsEndingWith("_STAIRS", "_SLABS"));

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

    private final Material expBottle = getMaterial("EXP_BOTTLE");
    private final Material spawner = getMaterial("MOB_SPAWNER");
}

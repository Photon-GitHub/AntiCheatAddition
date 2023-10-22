package de.photon.anticheataddition.util.minecraft.world.material;

import com.google.common.collect.Sets;
import de.photon.anticheataddition.ServerVersion;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.EnumSet;
import java.util.Set;

import static de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil.combineToImmutable;
import static de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil.ofTags;
import static org.bukkit.Material.*;

@Getter
public class OldMaterialUtil implements MaterialUtil
{
    private final Set<Material> airMaterials = ServerVersion.MC116.activeIsLaterOrEqual() ?
                                               Sets.immutableEnumSet(AIR, CAVE_AIR, VOID_AIR) :
                                               Sets.immutableEnumSet(AIR);

    private final Set<Material> autoStepMaterials = combineToImmutable(EnumSet.of(CHEST, TRAPPED_CHEST, ENDER_CHEST), ofTags(Tag.SLABS, Tag.STAIRS));
    private final Set<Material> bounceMaterials = combineToImmutable(EnumSet.of(SLIME_BLOCK), ofTags(Tag.BEDS));
    private final Set<Material> freeSpaceContainers = combineToImmutable(EnumSet.of(CHEST, TRAPPED_CHEST, ENDER_CHEST), ofTags(Tag.SHULKER_BOXES));
    private final Set<Material> nonOpenableInventories = Set.of();

    private final Set<Material> liquids = Sets.immutableEnumSet(WATER, LAVA);
    private final Set<Material> signs = ServerVersion.MC119.activeIsLaterOrEqual() ? ofTags(Tag.ALL_SIGNS) : ofTags(Tag.SIGNS, Tag.STANDING_SIGNS, Tag.WALL_SIGNS);

    private final Material expBottle = getMaterial("EXP_BOTTLE");
    private final Material spawner = getMaterial("MOB_SPAWNER");
}

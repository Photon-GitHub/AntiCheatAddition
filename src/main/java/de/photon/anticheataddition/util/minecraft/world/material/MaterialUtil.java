package de.photon.anticheataddition.util.minecraft.world.material;

import com.google.common.collect.Sets;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.datastructure.SetUtil;
import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.Arrays;
import java.util.Set;

import static org.bukkit.Material.*;

public sealed interface MaterialUtil permits AncientMaterialUtil, ModernMaterialUtil, OldMaterialUtil
{
    MaterialUtil INSTANCE = ServerVersion.MC112.activeIsEarlierOrEqual() ? new AncientMaterialUtil() : (ServerVersion.MC119.activeIsEarlierOrEqual() ? new OldMaterialUtil() : new ModernMaterialUtil());

    Set<Material> getAirMaterials();

    Set<Material> getAutoStepMaterials();

    Set<Material> getBounceMaterials();

    default Set<Material> getChangedHitboxMaterials()
    {
        return Set.of();
    }

    Set<Material> getFreeSpaceContainers();

    Set<Material> getLiquids();

    Set<Material> getNonOpenableInventories();

    Set<Material> getSigns();

    default Material getExpBottle()
    {
        return EXPERIENCE_BOTTLE;
    }

    default Material getSpawner()
    {
        return SPAWNER;
    }

    default boolean isAir(Material material)
    {
        return getAirMaterials().contains(material);
    }

    default boolean isLiquid(Material material)
    {
        return getLiquids().contains(material);
    }

    /**
     * Fix for Spigot's broken occluding method.
     */
    default boolean isReallyOccluding(Material material)
    {
        return material != INSTANCE.getSpawner() && material != BARRIER && material.isOccluding();
    }

    static Set<Material> getMaterialsEndingWith(String... ends)
    {
        return Arrays.stream(values()).filter(material -> Arrays.stream(ends).anyMatch(material.name()::endsWith)).collect(SetUtil.toImmutableEnumSet());
    }

    @SafeVarargs
    static Set<Material> ofTags(Tag<Material>... tags)
    {
        return Arrays.stream(tags).map(Tag::getValues).flatMap(Set::stream).collect(SetUtil.toImmutableEnumSet());
    }

    /**
     * Combines two Sets of Materials to an immutable enum set.
     *
     * @param materialOne The first set of materials. Will be MODIFIED.
     * @param materialTwo The second set of materials. Will not be modified.
     *
     * @return An immutable enum set containing all materials from both sets.
     */
    static Set<Material> combineToImmutable(Set<Material> materialOne, Set<Material> materialTwo)
    {
        materialOne.addAll(materialTwo);
        return Sets.immutableEnumSet(materialOne);
    }
}

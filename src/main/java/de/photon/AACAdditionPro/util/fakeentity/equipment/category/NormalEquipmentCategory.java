package de.photon.AACAdditionPro.util.fakeentity.equipment.category;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

/**
 * @author geNAZt
 */
public class NormalEquipmentCategory extends EquipmentCategory
{
    @Override
    public void load()
    {
        this.materials.addAll(Arrays.asList(
                // Air
                Material.AIR,

                // Wooden equip
                Material.WOOD_SWORD,
                Material.WOOD_AXE,
                Material.WOOD_PICKAXE,
                Material.WOOD_SPADE,
                Material.WOOD_HOE,

                // Stone equip
                Material.STONE_SWORD,
                Material.STONE_AXE,
                Material.STONE_PICKAXE,
                Material.STONE_SPADE,
                Material.STONE_HOE,

                // Iron Equip
                Material.IRON_SWORD,
                Material.IRON_AXE,
                Material.IRON_PICKAXE,
                Material.IRON_SPADE,
                Material.IRON_HOE,

                // Diamond equip
                Material.DIAMOND_SWORD,
                Material.DIAMOND_AXE,
                Material.DIAMOND_PICKAXE,
                Material.DIAMOND_SPADE,
                Material.DIAMOND_HOE,

                // Other tools
                Material.FLINT_AND_STEEL,
                Material.BOW,
                Material.FISHING_ROD,

                // PvP-stuff (Soups, Pots, etc)
                Material.GOLDEN_APPLE,
                Material.BOWL,
                Material.POTION,
                Material.GLASS_BOTTLE,
                Material.EXP_BOTTLE,

                // Building blocks
                Material.SANDSTONE,
                Material.SANDSTONE_STAIRS,

                // MC-Standard blocks
                Material.DIRT,
                Material.GRASS,
                Material.STONE,
                Material.BEDROCK,
                Material.COAL,
                Material.IRON_ORE,
                Material.GOLD_ORE,

                // Working material
                Material.GOLD_INGOT,
                Material.IRON_INGOT,
                Material.DIAMOND,
                Material.WOOD));
    }

    @Override
    public boolean isValid()
    {
        return this.materials.size() > 0;
    }

    @Override
    public List<Material> getMaterials()
    {
        return this.materials;
    }
}

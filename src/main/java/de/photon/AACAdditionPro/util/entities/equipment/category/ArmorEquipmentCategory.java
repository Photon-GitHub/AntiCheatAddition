package de.photon.AACAdditionPro.util.entities.equipment.category;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

/**
 * @author geNAZt
 */
public class ArmorEquipmentCategory extends EquipmentCategory
{
    @Override
    public void load()
    {
        this.materials.addAll(Arrays.asList(
                // Leather
                Material.LEATHER_HELMET,
                Material.LEATHER_CHESTPLATE,
                Material.LEATHER_LEGGINGS,
                Material.LEATHER_BOOTS,

                // Gold
                Material.GOLD_HELMET,
                Material.GOLD_CHESTPLATE,
                Material.GOLD_LEGGINGS,
                Material.GOLD_BOOTS,

                // Chain
                Material.CHAINMAIL_HELMET,
                Material.CHAINMAIL_CHESTPLATE,
                Material.CHAINMAIL_LEGGINGS,
                Material.CHAINMAIL_BOOTS,

                // Iron
                Material.IRON_HELMET,
                Material.IRON_CHESTPLATE,
                Material.IRON_LEGGINGS,
                Material.IRON_BOOTS,

                // Diamond
                Material.DIAMOND_HELMET,
                Material.DIAMOND_CHESTPLATE,
                Material.DIAMOND_LEGGINGS,
                Material.DIAMOND_BOOTS));
    }

    @Override
    public boolean isValid()
    {
        // We need at least one armor content
        return this.materials.size() > 0;
    }

    @Override
    public List<Material> getMaterials()
    {
        return this.materials;
    }
}

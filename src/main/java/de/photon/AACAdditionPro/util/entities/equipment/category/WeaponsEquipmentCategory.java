package de.photon.AACAdditionPro.util.entities.equipment.category;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;

/**
 * @author geNAZt
 */
public class WeaponsEquipmentCategory extends EquipmentCategory
{
    @Override
    public void load()
    {
        this.materials.addAll(Arrays.asList(
                Material.WOOD_SWORD,
                Material.WOOD_AXE,
                Material.GOLD_SWORD,
                Material.GOLD_AXE,
                Material.STONE_SWORD,
                Material.STONE_AXE,
                Material.IRON_SWORD,
                Material.IRON_AXE,
                Material.DIAMOND_SWORD,
                Material.DIAMOND_AXE));
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

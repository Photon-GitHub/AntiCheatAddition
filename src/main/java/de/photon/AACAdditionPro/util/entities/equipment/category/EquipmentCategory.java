package de.photon.AACAdditionPro.util.entities.equipment.category;

import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

/**
 * @author geNAZt
 */
public abstract class EquipmentCategory
{
    protected List<Material> materials = new ArrayList<>();

    /**
     * Load and init the default materials into this category
     */
    public abstract void load();

    /**
     * Decides if a category contains enough data to output for a equip run
     *
     * @return true if enough data is available, false if not
     */
    public abstract boolean isValid();

    /**
     * Get all currently loaded materials of this category
     *
     * @return list of materials in this category
     */
    public abstract List<Material> getMaterials();
}

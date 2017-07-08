package de.photon.AACAdditionPro.util.entities.equipment;

import com.comphenix.protocol.wrappers.EnumWrappers;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Material;

@RequiredArgsConstructor
public enum EquipmentMapping
{
    // The armor MUST be in first place here in this particular order to make some stuff with .ordinal() work
    HELMET("HELMET", EnumWrappers.ItemSlot.HEAD),
    CHESTPLATE("CHESTPLATE", EnumWrappers.ItemSlot.CHEST),
    LEGGINGS("LEGGINGS", EnumWrappers.ItemSlot.LEGS),
    BOOTS("BOOTS", EnumWrappers.ItemSlot.FEET);

    @Getter
    private final String key;

    @Getter
    private final EnumWrappers.ItemSlot slotOfItem;

    /**
     * Gets the correct {@link EquipmentMapping} for the {@link org.bukkit.inventory.ItemStack} with the given {@link Material}
     *
     * @param material the {@link Material} of the {@link org.bukkit.inventory.ItemStack}
     *
     * @return the {@link EquipmentMapping} to correctly set the armor.
     */
    public static EquipmentMapping getEquipmentMappingOfMaterial(Material material)
    {
        final String nameOfMaterial = material.name();
        for (EquipmentMapping equipmentMapping : EquipmentMapping.values()) {
            // endsWith here as of the better performance and all categories (SWORDS, AXES, etc.) are suffixes.
            if (nameOfMaterial.endsWith(equipmentMapping.getKey())) {
                return equipmentMapping;
            }
        }
        throw new IllegalStateException("No EquipmentMapping was found for Material " + material);
    }
}

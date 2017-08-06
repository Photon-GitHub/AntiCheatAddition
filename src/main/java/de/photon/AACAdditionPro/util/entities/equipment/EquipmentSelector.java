package de.photon.AACAdditionPro.util.entities.equipment;

import de.photon.AACAdditionPro.util.entities.ClientsideEntity;
import de.photon.AACAdditionPro.util.entities.equipment.category.ArmorEquipmentCategory;
import de.photon.AACAdditionPro.util.entities.equipment.category.NormalEquipmentCategory;
import org.bukkit.Material;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author geNAZt
 */
public class EquipmentSelector
{
    private final EquipmentDatabase database;

    public EquipmentSelector(EquipmentDatabase database)
    {
        this.database = database;
    }

    /**
     * Gets an Array of {@link Material}s that represents the armor a player could possibly have.
     * [0] == Helmet
     * [1] == Chestplate
     * [2] == Leggings
     * [3] == Boots
     */
    Material[] selectArmor(ClientsideEntity entity)
    {
        // Check if the state of the armor database is correct
        ArmorEquipmentCategory category = this.database.getCategory(ArmorEquipmentCategory.class, entity);
        if ( category != null && category.isValid() ) {
            final List<Material> armorMaterials = category.getMaterials();
            final Material[] armor = new Material[4];

            for (byte b = 0; b < (byte) 6; b++) {
                final Material randomArmorMaterial = armorMaterials.get(ThreadLocalRandom.current().nextInt(armorMaterials.size()));
                if (randomArmorMaterial == null) {
                    continue;
                }

                // The armor is always in the first place, therefore the ordinal works here.
                armor[EquipmentMapping.getEquipmentMappingOfMaterial(randomArmorMaterial).ordinal()] = randomArmorMaterial;
            }

            return armor;
        }

        return new Material[4];
    }

    /**
     * Get a item for the main hand
     */
    Material selectMainHand(ClientsideEntity entity)
    {
        // Decide from which pool we select (fight or normal)
        boolean fight = ThreadLocalRandom.current().nextBoolean();

        if (fight) {
            NormalEquipmentCategory category = this.database.getCategory(NormalEquipmentCategory.class, entity);
            return category != null ? category.getMaterials().get(ThreadLocalRandom.current().nextInt(category.getMaterials().size())) : Material.AIR;
        } else {
            NormalEquipmentCategory category = this.database.getCategory(NormalEquipmentCategory.class, entity);
            return category != null ? category.getMaterials().get(ThreadLocalRandom.current().nextInt(category.getMaterials().size())) : Material.AIR;
        }
    }
}

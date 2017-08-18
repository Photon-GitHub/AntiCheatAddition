package de.photon.AACAdditionPro.api.killauraentity;

/**
 * This is used to determine where to set an {@link org.bukkit.inventory.ItemStack} in the {@link org.bukkit.inventory.Inventory}
 * of the {@link de.photon.AACAdditionPro.util.entities.ClientsidePlayerEntity}
 */
public enum KillauraEntityEquipmentCategory
{
    ARMOR,
    MAIN_HAND,
    OFFHAND;

    /**
     * Gets the {@link KillauraEntityEquipmentCategory} from the name of the {@link org.bukkit.configuration.ConfigurationSection}.
     *
     * @param configSection the name of the {@link org.bukkit.configuration.ConfigurationSection}.
     *
     * @return the {@link KillauraEntityEquipmentCategory} of the {@link org.bukkit.configuration.ConfigurationSection} or MAIN_HAND if nothing was found.
     */
    public static KillauraEntityEquipmentCategory getEquipmentByConfigSection(final String configSection)
    {
        // Loop through all categories to filter out the correct one
        for (KillauraEntityEquipmentCategory category : KillauraEntityEquipmentCategory.values()) {
            if (category.name().equalsIgnoreCase(configSection)) {
                return category;
            }
        }

        // TODO: OFFHAND
        return MAIN_HAND;
    }
}

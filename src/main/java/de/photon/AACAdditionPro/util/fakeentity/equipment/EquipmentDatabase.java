package de.photon.AACAdditionPro.util.fakeentity.equipment;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.api.killauraentity.KillauraEntityEquipmentCategory;
import de.photon.AACAdditionPro.events.KillauraEntityEquipmentPrepareEvent;
import de.photon.AACAdditionPro.util.fakeentity.ClientsideEntity;
import de.photon.AACAdditionPro.util.fakeentity.equipment.category.EquipmentCategory;
import de.photon.AACAdditionPro.util.files.ConfigUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class EquipmentDatabase
{
    // We ensure that getCategory can only we called from the main thread. so we are sure only one write at a time can happen
    private final Map<Class<? extends EquipmentCategory>, Constructor<? extends EquipmentCategory>> constructorCache = new HashMap<>();

    /**
     * Get a new category instance. A instance holds materials which should be used to randomize a material out of. This
     * also removes materials revoked by the config and lets the user decide what materials to finally use via the
     * {@link KillauraEntityEquipmentPrepareEvent}.
     *
     * @param categoryClass The class of the category
     * @param entity        For which entity this category is
     * @param <T>           Type of Category Object
     *
     * @return a already filtered category with materials ready to choose from
     */
    <T extends EquipmentCategory> T getCategory(Class<T> categoryClass, ClientsideEntity entity)
    {
        // Since we use a HashMap and a sync event we need to ensure that we are in the Bukkit MainThread
        Validate.isTrue(Bukkit.isPrimaryThread(), "getCategory call outside of the main thread");

        // This is somewhat expensive, we construct each category for each iteration.
        // This is done to keep the API as clean as possible to enable the customer to decide
        // which materials are in for a selection

        // Get the constructor from the cache, if its not there reflect it
        Constructor<? extends EquipmentCategory> constructor = constructorCache.computeIfAbsent(categoryClass, aClass -> {
            Constructor<? extends EquipmentCategory> reflectedConstructor = null;

            try {
                reflectedConstructor = aClass.getConstructor();
                reflectedConstructor.setAccessible(true); // Even if the constructor is public this disables
                // the security manager for performance reasons
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }

            return reflectedConstructor;
        });

        // Ok now we got the constructor, get a new instance and make the api handle the materials
        try {
            EquipmentCategory category = constructor.newInstance();
            category.load();

            // Override potential config settings and filter out affected materials
            final List<Material> materials = category.getMaterials();
            final String categoryName = categoryClass.getSimpleName().replace(EquipmentCategory.class.getSimpleName(), "").toLowerCase();
            final Set<String> optionKeys = ConfigUtils.loadKeys(ModuleType.KILLAURA_ENTITY.getConfigString() + ".equipment." + categoryName);

            for (final String optionKey : optionKeys) {
                if (!AACAdditionPro.getInstance().getConfig().getBoolean(ModuleType.KILLAURA_ENTITY.getConfigString() + ".equipment." + categoryName + "." + optionKey)) {
                    // Filter out the affected materials
                    materials.removeIf((material -> material.name().contains(optionKey.toUpperCase())));
                }
            }

            // Fire a event which should always be sync
            Bukkit.getPluginManager().callEvent(
                    new KillauraEntityEquipmentPrepareEvent(
                            entity.getObservedPlayer(),
                            // Get the category of the config-section name
                            KillauraEntityEquipmentCategory.getEquipmentByConfigSection(categoryName),
                            materials)
                                               );

            return (T) category;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

        // This should never happen because the no arg constructors never have code in them
        return null;
    }
}

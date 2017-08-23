package de.photon.AACAdditionPro.events;

import de.photon.AACAdditionPro.api.killauraentity.KillauraEntityEquipmentCategory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import java.util.List;

/**
 * This event is called once a {@link de.photon.AACAdditionPro.util.entities.ClientsidePlayerEntity} modifies an item in
 * its {@link org.bukkit.inventory.Inventory}. The replacing {@link Material} will be chosen from the provided {@link List}
 * of {@link Material}s. The {@link KillauraEntityEquipmentCategory} enables you to see the usage of the {@link org.bukkit.entity.Item}.
 * <p>
 * E.g. After intercepting an Event with the {@link KillauraEntityEquipmentCategory} ARMOR you can modify the {@link List}
 * of armor materials you can influence the result by removing or adding {@link Material}s to the {@link List}, or even
 * force a {@link Material} by removing all other entries.
 */
public class KillauraEntityEquipmentPrepareEvent extends PlayerEvent
{
    private static final HandlerList handlers = new HandlerList();
    private final KillauraEntityEquipmentCategory category;
    private final List<Material> materials;

    public KillauraEntityEquipmentPrepareEvent(Player who, KillauraEntityEquipmentCategory category, List<Material> materials)
    {
        super(who);
        this.category = category;
        this.materials = materials;
    }

    /**
     * Returns the category of the materials
     *
     * @return category of the materials
     */
    public KillauraEntityEquipmentCategory getCategory()
    {
        return this.category;
    }

    /**
     * List of {@link Material}s the resulting {@link Material} will be chosen from.
     *
     * @return the list is fully mutable, you may add or remove materials
     */
    public List<Material> getMaterials()
    {
        return this.materials;
    }

    // Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }
}

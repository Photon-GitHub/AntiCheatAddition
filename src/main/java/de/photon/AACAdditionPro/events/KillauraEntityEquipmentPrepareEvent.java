package de.photon.AACAdditionPro.events;

import de.photon.AACAdditionPro.api.KillauraEntityEquipmentCategory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;

import java.util.List;

/**
 * @author geNAZt
 *
 * This event gets fired when a Killaura Entity decides to take a new item in its inventory. The List given
 * represents the Materials it can choose from. The Category shows for what the list is.
 *
 * For example you get a event with Category "Armor" and a list of armor materials, you can decide which
 * armor the entity should take by removing all other options in the list. If you remove all options the entity
 * won't change its inventory.
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
    public KillauraEntityEquipmentCategory getCategory() {
        return this.category;
    }

    /**
     * List of materials the entity will select from
     *
     * @return the list is fully mutable, you may add or remove materials
     */
    public List<Material> getMaterials() {
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

package de.photon.AACAdditionPro.events;

import com.comphenix.protocol.wrappers.EnumWrappers;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * This event is called every time a {@link de.photon.AACAdditionPro.util.fakeentity.ClientsidePlayerEntity} modifies
 * an item in its {@link org.bukkit.inventory.Inventory}.<br>
 * The {@link Material} of the new item will be chosen from the
 * provided {@link List} of {@link Material}s. The {@link com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot}
 * allows to know the replaced item slot. <br>
 * If you remove all but one {@link Material} from the {@link List} of {@link Material}, you force that {@link Material}
 */
public class KillauraEntityEquipmentPrepareEvent extends PlayerEvent
{
    private static final HandlerList handlers = new HandlerList();
    @Getter
    private final EnumWrappers.ItemSlot itemSlot;
    @Getter
    @Setter
    private List<Material> materials;

    public KillauraEntityEquipmentPrepareEvent(Player who, EnumWrappers.ItemSlot itemSlot, List<Material> materials)
    {
        super(who);
        this.itemSlot = itemSlot;
        this.materials = materials;
    }

    // Needed for 1.8.8
    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    @NotNull
    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }
}

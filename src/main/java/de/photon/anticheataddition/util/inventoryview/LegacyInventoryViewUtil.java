package de.photon.anticheataddition.util.inventoryview;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

final class LegacyInventoryViewUtil implements InventoryViewUtil
{
    @Override
    public InventoryView createInventoryView(Inventory top, Inventory bottom, Player player, InventoryType type, String title)
    {
        return player.getOpenInventory();
    }
}

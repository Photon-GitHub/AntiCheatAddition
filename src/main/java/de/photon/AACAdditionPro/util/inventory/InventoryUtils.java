package de.photon.AACAdditionPro.util.inventory;

import de.photon.AACAdditionPro.AACAdditionPro;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class InventoryUtils
{

    /**
     * This schedules an {@link org.bukkit.inventory.Inventory} update to be executed synchronously in the next server tick
     *
     * @param player the {@link Player} who's {@link org.bukkit.inventory.Inventory} should be updated.
     */
    public static void syncUpdateInventory(Player player)
    {
        Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), player::updateInventory);
    }
}

package de.photon.anticheataddition.util.inventoryview;

import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public interface InventoryViewUtil
{
    InventoryViewUtil INSTANCE = ServerVersion.MC119.activeIsLaterOrEqual() ? new LegacyInventoryViewUtil() : new ModernInventoryViewUtil();

    /**
     * Tries to create a test {@link InventoryView} for a {@link User}.
     * If the server version is too old, the InventoryView will only be the internal crafting inventory of the player!
     */
    default InventoryView createTestView(User user)
    {
        final Inventory top = Bukkit.createInventory(user.getPlayer(), InventoryType.CHEST);
        final Inventory bottom = user.getPlayer().getInventory();

        // Fill the top inventory with stone.
        top.addItem(new ItemStack(Material.STONE, 1700));

        return createInventoryView(top, bottom, user.getPlayer(), InventoryType.CHEST, "TestInventory");
    }

    /**
     * This tries to create a new {@link InventoryView} as per specification.
     * If the server version is too old, the {@link Player#getOpenInventory()} method is used to create the {@link InventoryView}, which will not contain the parameters.
     */
    InventoryView createInventoryView(Inventory top, Inventory bottom, Player player, InventoryType type, String title);
}

package de.photon.anticheataddition.util.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

final class ModernInventoryUtil implements InventoryUtil
{
    public List<ItemStack> getHandContents(final Player player)
    {
        return List.of(player.getInventory().getItemInMainHand(), player.getInventory().getItemInOffHand());
    }

    public Optional<SlotLocation> locateSlot(int rawSlot, final Inventory inventory) throws IllegalArgumentException
    {
        // Invalid slot (including the -999 outside raw slot constant)
        if (rawSlot < 0) return Optional.empty();

        final int size = inventory.getSize();
        switch (inventory.getType()) {
            case CHEST:
            case ENDER_CHEST:
            case BARREL:
            case SHULKER_BOX:
                /*
                 * 0                         -                       8
                 *
                 * 9                         -                       17
                 *
                 * 18                        -                       26
                 *
                 * ------------------------------------------------------
                 * ------------------------------------------------------
                 * 27                        -                       35
                 *
                 * 36                        -                       44
                 *
                 * 45                        -                       53
                 * ------------------------------------------------------
                 * 54                        -                       62
                 */
                // There are custom chest sizes (from 0 to 54!)
                if (rawSlot < size) return SlotLocation.opOf(rawSlot % 9, rawSlot / 9D);
                return InventoryUtil.lowerInventoryLocation(size, rawSlot);
            case DISPENSER:
            case DROPPER:
                // Dispenser and Dropper have the same layout
                /*
                 *                   0       1       2
                 *
                 *                   3       4       5
                 *
                 *                   6       7       8
                 *
                 * ------------------------------------------------------
                 * ------------------------------------------------------
                 * 9                         -                       17
                 *
                 * 18                        -                       26
                 *
                 * 27                        -                       35
                 * ------------------------------------------------------
                 * 36                        -                       44
                 */
                // In the dispenser - part
                if (rawSlot < 9) return SlotLocation.opOf(4D + rawSlot % 3, rawSlot / 3D);
                return InventoryUtil.lowerInventoryLocation(9, rawSlot);
            case FURNACE:
            case BLAST_FURNACE:
            case SMOKER:
                /*
                 *                 0
                 *
                 *                                    2
                 *
                 *                 1
                 *
                 * ------------------------------------------------------
                 * ------------------------------------------------------
                 * 3                         -                       11
                 *
                 * 12                        -                       20
                 *
                 * 21                        -                       29
                 * ------------------------------------------------------
                 * 30                        -                       38
                 */
                switch (rawSlot) {
                    case 0: return SlotLocation.opOf(2.5, 0);
                    case 1: return SlotLocation.opOf(2.5, 2);
                    case 2: return SlotLocation.opOf(6, 1);
                    default: return InventoryUtil.lowerInventoryLocation(3, rawSlot);
                }
            case WORKBENCH:
                /*
                 *          1       2       3
                 *
                 *          4       5       6            0
                 *
                 *          7       8       9
                 *
                 * ------------------------------------------------------
                 * ------------------------------------------------------
                 * 10                        -                       18
                 *
                 * 19                        -                       27
                 *
                 * 28                        -                       36
                 * ------------------------------------------------------
                 * 37                        -                       45
                 */
                if (rawSlot == 0) return SlotLocation.opOf(6.5, 1);

                if (rawSlot <= 9) {
                    final double x = ((rawSlot - 1) % 3) + 1.25D;
                    final double y = ((rawSlot - 1D) / 3D);
                    return SlotLocation.opOf(x, y);
                }

                return InventoryUtil.lowerInventoryLocation(10, rawSlot);
            case ENCHANTING:
                /*
                 *
                 *
                 *    0    1
                 *
                 *
                 * ------------------------------------------------------
                 * ------------------------------------------------------
                 * 2                         -                       10
                 *
                 * 11                        -                       19
                 *
                 * 20                        -                       28
                 * ------------------------------------------------------
                 * 29                        -                       37
                 */
                switch (rawSlot) {
                    case 0: return SlotLocation.opOf(0.4, 2.6);
                    case 1: return SlotLocation.opOf(1.5, 2.6);
                    default: return InventoryUtil.lowerInventoryLocation(2, rawSlot);
                }
            case MERCHANT:
                /*
                 *
                 *
                 *       0           1                      2
                 *
                 *
                 * ------------------------------------------------------
                 * ------------------------------------------------------
                 * 3                         -                       11
                 *
                 * 12                        -                       20
                 *
                 * 21                        -                       29
                 * ------------------------------------------------------
                 * 30                        -                       38
                 */
                switch (rawSlot) {
                    case 0: return SlotLocation.opOf(1.5, 1);
                    case 1: return SlotLocation.opOf(3, 1);
                    case 2: return SlotLocation.opOf(6.3, 1);
                    default: return InventoryUtil.lowerInventoryLocation(3, rawSlot);
                }
            case ANVIL:
            case SMITHING:
                /*
                 *
                 *
                 *       0                 1                      2
                 *
                 *
                 * ------------------------------------------------------
                 * ------------------------------------------------------
                 * 3                         -                       11
                 *
                 * 12                        -                       20
                 *
                 * 21                        -                       29
                 * ------------------------------------------------------
                 * 30                        -                       38
                 */
                switch (rawSlot) {
                    case 0: return SlotLocation.opOf(1, 1.5);
                    case 1: return SlotLocation.opOf(3.6, 1.5);
                    case 2: return SlotLocation.opOf(7, 1.5);
                    default: return InventoryUtil.lowerInventoryLocation(3, rawSlot);
                }
            case BEACON:
                /*
                 *
                 *
                 *                                        0
                 *
                 *
                 * ------------------------------------------------------
                 * ------------------------------------------------------
                 * 1                         -                       9
                 *
                 * 10                        -                       18
                 *
                 * 19                        -                       27
                 * ------------------------------------------------------
                 * 28                        -                       36
                 */
                if (rawSlot == 0) return SlotLocation.opOf(5.5, 2);
                return InventoryUtil.lowerInventoryLocation(1, rawSlot);
            case HOPPER:
                /*
                 *
                 *
                 *           0       1       2       3       4
                 *
                 *
                 * ------------------------------------------------------
                 * ------------------------------------------------------
                 * 5                         -                       13
                 *
                 * 14                        -                       22
                 *
                 * 23                        -                       31
                 * ------------------------------------------------------
                 * 32                        -                       40
                 */
                if (rawSlot <= 4) return SlotLocation.opOf(2D + rawSlot, 1D);
                return InventoryUtil.lowerInventoryLocation(5, rawSlot);

            case BREWING:
                /*
                 *      4                    3
                 *
                 *
                 *                     0           2
                 *                           1
                 * ------------------------------------------------------
                 * ------------------------------------------------------
                 * 5                         -                       13
                 *
                 * 14                        -                       22
                 *
                 * 23                        -                       31
                 * ------------------------------------------------------
                 * 32                        -                       40
                 */
                switch (rawSlot) {
                    case 0: return SlotLocation.opOf(5.3, 1.5);
                    case 1: return SlotLocation.opOf(4, 2);
                    case 2: return SlotLocation.opOf(2.6, 1.5);
                    case 3: return SlotLocation.opOf(4, 0);
                    case 4: return SlotLocation.opOf(0.5, 0);
                    default: return InventoryUtil.lowerInventoryLocation(5, rawSlot);
                }
                /*
                 *      0
                 *
                 *                                          2
                 *      1
                 *
                 * ------------------------------------------------------
                 * ------------------------------------------------------
                 * 5                         -                       13
                 *
                 * 14                        -                       22
                 *
                 * 23                        -                       31
                 * ------------------------------------------------------
                 * 32                        -                       40
                 */
            case CARTOGRAPHY:
                switch (rawSlot) {
                    case 0: return SlotLocation.opOf(0.5, 0);
                    case 1: return SlotLocation.opOf(0.5, 2);
                    case 2: return SlotLocation.opOf(7.5, 1.3);
                    default: return InventoryUtil.lowerInventoryLocation(3, rawSlot);
                }
                /*
                 *                 0
                 *
                 *                 1                         2
                 *
                 *
                 * ------------------------------------------------------
                 * ------------------------------------------------------
                 * 5                         -                       13
                 *
                 * 14                        -                       22
                 *
                 * 23                        -                       31
                 * ------------------------------------------------------
                 * 32                        -                       40
                 */
            case GRINDSTONE:
                switch (rawSlot) {
                    case 0: return SlotLocation.opOf(2.3, 0);
                    case 1: return SlotLocation.opOf(2.3, 1);
                    case 2: return SlotLocation.opOf(6.6, 0.6);
                    default: return InventoryUtil.lowerInventoryLocation(3, rawSlot);
                }
            case CRAFTING:
            case PLAYER:
                /* Player Inventory
                 * 5
                 * 6                            1   2
                 *                                         ->      0
                 * 7                            3   4
                 *
                 * 8                       45
                 * ------------------------------------------------------
                 * 9                         -                       17
                 *
                 * 18                        -                       26
                 *
                 * 27                        -                       35
                 * ------------------------------------------------------
                 * 36                        -                       44
                 * */
                switch (rawSlot) {
                    // Crafting slots
                    case 0: return SlotLocation.opOf(7.5, 1.5);
                    case 1: return SlotLocation.opOf(4.5, 1);
                    case 2: return SlotLocation.opOf(5.5, 1);
                    case 3: return SlotLocation.opOf(4.5, 2);
                    case 4: return SlotLocation.opOf(5.5, 2);
                    // Armor slots.
                    case 5:
                    case 6:
                    case 7:
                    case 8: return SlotLocation.opOf(0D, rawSlot - 5D);
                    // Shield slot.
                    case 45: return SlotLocation.opOf(3.6D, 3);
                    default: return InventoryUtil.lowerInventoryLocation(9, rawSlot);
                }
            default:
                // CREATIVE (false positives), and a few rare inventories (like looms).
                return Optional.empty();
        }
    }
}

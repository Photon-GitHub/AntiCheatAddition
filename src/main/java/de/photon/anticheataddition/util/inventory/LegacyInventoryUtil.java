package de.photon.anticheataddition.util.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

public class LegacyInventoryUtil implements InventoryUtil
{
    @Override
    public List<ItemStack> getHandContents(Player player)
    {
        return List.of(player.getInventory().getItemInHand());
    }

    @Override
    public Optional<SlotLocation> locateSlot(int rawSlot, final Inventory inventory) throws IllegalArgumentException
    {
        // Invalid slot (including the -999 outside raw slot constant)
        if (rawSlot < 0) return Optional.empty();

        final int size = inventory.getSize();
        switch (inventory.getType()) {
            case CHEST:
            case ENDER_CHEST:
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
                    case 0: return SlotLocation.opOf(2.5D, 0D);
                    case 1: return SlotLocation.opOf(2.5D, 2D);
                    case 2: return SlotLocation.opOf(6D, 1D);
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

                if (rawSlot == 0) return SlotLocation.opOf(6.5D, 1D);

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
                    case 0: return SlotLocation.opOf(0.4D, 2.6D);
                    case 1: return SlotLocation.opOf(1.5D, 2.6D);
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
                break;
            case ANVIL:
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
                    case 0: return SlotLocation.opOf(1D, 1.5D);
                    case 1: return SlotLocation.opOf(3.6D, 1.5D);
                    case 2: return SlotLocation.opOf(7D, 1.5D);
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
                if (rawSlot == 0) return SlotLocation.opOf(5.5D, 2D);
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
                // Start at y = 1 as the inventory is smaller
                if (rawSlot <= 4) return SlotLocation.opOf(2D + rawSlot, 1D);
                return InventoryUtil.lowerInventoryLocation(5, rawSlot);
            case CRAFTING:
            case PLAYER:
                /* Player Inventory
                 * 5
                 * 6                            1   2
                 *                                         ->      0
                 * 7                            3   4
                 *
                 * 8
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
                    case 0: return SlotLocation.opOf(7.5D, 1.5D);
                    case 1: return SlotLocation.opOf(4.5D, 1D);
                    case 2: return SlotLocation.opOf(5.5D, 1D);
                    case 3: return SlotLocation.opOf(4.5D, 2D);
                    case 4: return SlotLocation.opOf(5.5D, 2D);
                    // Armor slots.
                    case 5:
                    case 6:
                    case 7:
                    case 8: return SlotLocation.opOf(0D, rawSlot - 5);
                    default: return InventoryUtil.lowerInventoryLocation(9, rawSlot);
                }
            default:
                // CREATIVE (false positives), PLAYER, SHULKER_BOX, BREWING_STAND (version compatibility)
                return Optional.empty();
        }
        return Optional.empty();
    }
}

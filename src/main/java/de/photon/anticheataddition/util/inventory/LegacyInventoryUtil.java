package de.photon.anticheataddition.util.inventory;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Optional;

final class LegacyInventoryUtil implements InventoryUtil
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
            case CHEST, ENDER_CHEST, SHULKER_BOX -> {
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
                final int row = rawSlot / 9;
                if (rawSlot < size) return SlotLocation.opOf(rawSlot % 9, row);
                return InventoryUtil.lowerInventoryLocation(size, rawSlot);
            }
            case DISPENSER, DROPPER -> {
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
                final int row = rawSlot / 3;
                if (rawSlot < 9) return SlotLocation.opOf(4D + rawSlot % 3, row);
                return InventoryUtil.lowerInventoryLocation(9, rawSlot);
            }
            case FURNACE -> {
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
                return switch (rawSlot) {
                    case 0 -> SlotLocation.opOf(2.5D, 0D);
                    case 1 -> SlotLocation.opOf(2.5D, 2D);
                    case 2 -> SlotLocation.opOf(6D, 1D);
                    default -> InventoryUtil.lowerInventoryLocation(3, rawSlot);
                };
            }
            case WORKBENCH -> {
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

                return switch (rawSlot) {
                    case 0 -> SlotLocation.opOf(6.5D, 1D);
                    case 1, 2, 3 -> SlotLocation.opOf(rawSlot + 0.25, 0D);
                    case 4, 5, 6 -> SlotLocation.opOf(rawSlot + 0.25, 1D);
                    case 7, 8, 9 -> SlotLocation.opOf(rawSlot + 0.25, 2D);
                    default -> InventoryUtil.lowerInventoryLocation(10, rawSlot);
                };
            }
            case ENCHANTING -> {
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
                return switch (rawSlot) {
                    case 0 -> SlotLocation.opOf(0.4D, 2.6D);
                    case 1 -> SlotLocation.opOf(1.5D, 2.6D);
                    default -> InventoryUtil.lowerInventoryLocation(2, rawSlot);
                };
            }
            case ANVIL -> {
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
                return switch (rawSlot) {
                    case 0 -> SlotLocation.opOf(1D, 1.5D);
                    case 1 -> SlotLocation.opOf(3.6D, 1.5D);
                    case 2 -> SlotLocation.opOf(7D, 1.5D);
                    default -> InventoryUtil.lowerInventoryLocation(3, rawSlot);
                };
            }
            case BEACON -> {
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
            }
            case HOPPER -> {
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
            }
            case CRAFTING, PLAYER -> {
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
                return switch (rawSlot) {
                    // Crafting slots
                    case 0 -> SlotLocation.opOf(7.5D, 1.5D);
                    case 1 -> SlotLocation.opOf(4.5D, 1D);
                    case 2 -> SlotLocation.opOf(5.5D, 1D);
                    case 3 -> SlotLocation.opOf(4.5D, 2D);
                    case 4 -> SlotLocation.opOf(5.5D, 2D);
                    // Armor slots.
                    case 5, 6, 7, 8 -> SlotLocation.opOf(0D, rawSlot - 5D);
                    default -> InventoryUtil.lowerInventoryLocation(9, rawSlot);
                };
            }
            default -> {
                // CREATIVE (false positives), PLAYER, SHULKER_BOX, BREWING_STAND (version compatibility)
                return Optional.empty();
            }
        }
    }
}

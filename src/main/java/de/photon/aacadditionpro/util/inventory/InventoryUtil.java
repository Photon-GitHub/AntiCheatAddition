package de.photon.aacadditionpro.util.inventory;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.util.mathematics.MathUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InventoryUtil
{
    /**
     * Gets the content of the main hand for version 1.8.8 or the content of both hands in higher versions.
     *
     * @return an {@link List} which contains all the {@link ItemStack}s a player has: 1 in MC 1.8.8 and 2 in higher versions.
     */
    public static List<ItemStack> getHandContents(final Player player)
    {
        return ServerVersion.getActiveServerVersion() == ServerVersion.MC18 ?
               List.of(player.getInventory().getItemInHand()) :
               List.of(player.getInventory().getItemInMainHand(),
                       player.getInventory().getItemInOffHand());

    }

    /**
     * Checks if an {@link Inventory} is empty.
     */
    public static boolean isInventoryEmpty(final Inventory inventory)
    {
        for (ItemStack content : inventory.getContents()) {
            if (content != null) return false;
        }
        return true;
    }

    /**
     * Calculates the distance between two raw slots.
     *
     * @param rawSlotOne    the first (raw) slot
     * @param rawSlotTwo    the second (raw) slot
     * @param inventoryType the inventory layout when the click happened.
     *
     * @return the distance between the two slots or -1 if locating the slots failed.
     */
    public static double distanceBetweenSlots(final int rawSlotOne, final int rawSlotTwo, @NotNull final InventoryType inventoryType)
    {
        val first = InventoryUtil.locateSlot(rawSlotOne, inventoryType);
        val second = InventoryUtil.locateSlot(rawSlotTwo, inventoryType);

        return first == null || second == null ? -1 : first.distance(second);
    }

    /**
     * Used to locate a slot in an {@link Inventory}.
     * The coordinate-system is (0|0) in upper-left corner.
     * <br>
     * Please make sure that the {@link Player} is not riding a horse, as this method does not support horses/donkeys/mules
     *
     * @param rawSlot       the number that is returned by getRawSlot()
     * @param inventoryType the inventory layout when the click happened.
     *
     * @return the coordinates of a slot or null if it is invalid.
     */
    @SuppressWarnings("IntegerDivisionInFloatingPointContext")
    @Nullable
    public static SlotLocation locateSlot(int rawSlot, final InventoryType inventoryType) throws IllegalArgumentException
    {
        // Invalid slot (including the -999 outside rawslot constant)
        if (rawSlot < 0) return null;

        double x;
        double y;
        switch (inventoryType) {
            case CHEST:
            case ENDER_CHEST:
                // TODO: MAKE SURE THAT THE PLAYER IS NOT RIDING A HORSE, THAT IS ALSO COUNTED AS CHEST.
                // Chest and Enderchest have the same layout
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

                x = rawSlot % 9;
                y = (rawSlot / 9);
                if (rawSlot >= 27) y += 0.5D; // Player inventory
                if (rawSlot >= 54) y += 0.25D; // Quickbar
                return new SlotLocation(x, y);
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
                if (rawSlot < 9) return new SlotLocation(4 + rawSlot % 3, rawSlot / 3);

                x = rawSlot % 9;
                // 3.5D is normal offset, but the rawslots in the player inventory start with 9, which will
                // automatically add 1 in the division -> offset 2.5.
                y = (rawSlot / 9) + 2.5D;
                if (rawSlot >= 36) y += 0.25D; // Quickbar
                return new SlotLocation(x, y);
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
                    case 0:
                        return new SlotLocation(2.5D, 0D);
                    case 1:
                        return new SlotLocation(2.5D, 2D);
                    case 2:
                        return new SlotLocation(6D, 1D);
                    default:
                        x = (rawSlot - 3) % 9;
                        y = ((rawSlot - 3) / 9) + 3.5D;
                        if (rawSlot >= 30) y += 0.25D; // Quickbar
                        return new SlotLocation(x, y);
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

                if (rawSlot == 0) return new SlotLocation(6.5D, 1D);

                if (rawSlot <= 9) {
                    x = ((rawSlot - 1) % 3) + 1.25D;
                    y = ((rawSlot - 1) / 3);
                    return new SlotLocation(x, y);
                }

                x = (rawSlot - 1) % 9;
                y = ((rawSlot - 1) / 9) + 2.5D;
                if (rawSlot >= 37) y += 0.25D; // Quickbar
                return new SlotLocation(x, y);

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
                break;
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
                break;
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
                break;
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
                if (rawSlot <= 4) return new SlotLocation(2D + rawSlot, 1D);
                x = (rawSlot - 5) % 9;
                y = ((rawSlot - 5) / 9) + 2.5D;
                if (rawSlot >= 32) y += 0.25D; // Quickbar
                return new SlotLocation(x, y);

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

                // Result slot
                if (rawSlot == 0) return new SlotLocation(7.5D, 1.5D);

                // Crafting slots
                if (rawSlot <= 4) return new SlotLocation(5.5D - (rawSlot % 2), rawSlot <= 2 ? 1D : 2D);

                // Armor slots
                if (rawSlot <= 8) return new SlotLocation(0D, rawSlot - 5);

                x = (rawSlot - 9) % 9;
                y = ((rawSlot - 9) / 9) + 4.25D;
                if (rawSlot >= 36) y += 0.25D; // Quickbar
                return new SlotLocation(x, y);
            case CREATIVE:
            case SHULKER_BOX:
                break;
            default:
                // CREATIVE (false positives), PLAYER, SHULKER_BOX, BREWING_STAND (version compatibility)
                return null;
        }
        return null;
    }

    /**
     * This schedules an {@link Inventory} update to be executed synchronously in the next server tick
     *
     * @param player the {@link Player} who's {@link Inventory} should be updated.
     */
    public static void syncUpdateInventory(Player player)
    {
        Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), player::updateInventory);
    }

    @Value
    public static class SlotLocation
    {
        public static final SlotLocation DUMMY = new SlotLocation(0, 0);

        double x;
        double y;

        public double distance(SlotLocation other)
        {
            return MathUtil.fastHypot(x - other.x, y - other.y);
        }
    }
}

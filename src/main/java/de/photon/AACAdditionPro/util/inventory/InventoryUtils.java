package de.photon.AACAdditionPro.util.inventory;

import com.google.common.collect.ImmutableList;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ServerVersion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class InventoryUtils
{
    /**
     * Gets the content of the main hand for version 1.8.8 or the content of both hands in higher versions.
     *
     * @return an {@link ImmutableList} which contains all the {@link ItemStack}s a player has: 1 in MC 1.8.8 and 2 in higher versions.
     */
    public static List<ItemStack> getHandContents(Player player)
    {
        switch (ServerVersion.getActiveServerVersion())
        {
            case MC188:
                return ImmutableList.of(player.getInventory().getItemInHand());
            case MC112:
            case MC113:
            case MC114:
                return ImmutableList.of(player.getInventory().getItemInMainHand(),
                                        player.getInventory().getItemInOffHand());
            default:
                throw new IllegalStateException("Unknown minecraft version");
        }
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
    public static double distanceBetweenSlots(final int rawSlotOne, final int rawSlotTwo, final InventoryType inventoryType)
    {
        double[] locationOfFirstClick = InventoryUtils.locateSlot(rawSlotOne, inventoryType);
        double[] locationOfSecondClick = InventoryUtils.locateSlot(rawSlotTwo, inventoryType);

        if (locationOfFirstClick == null || locationOfSecondClick == null)
        {
            return -1;
        }

        return Math.hypot(locationOfFirstClick[0] - locationOfSecondClick[0], locationOfFirstClick[1] - locationOfSecondClick[1]);
    }

    /**
     * Used to locate a slot in an {@link org.bukkit.inventory.Inventory}.
     * The coordinate-system is (0|0) in upper-left corner.
     * <br>
     * Please make sure that the {@link Player} is not riding a horse, as this method does not support horses/donkeys/mules
     *
     * @param rawSlot       the number that is returned by getRawSlot()
     * @param inventoryType the inventory layout when the click happened.
     *
     * @return the coordinates of a slot or null if it is invalid.
     */
    public static double[] locateSlot(int rawSlot, final InventoryType inventoryType) throws IllegalArgumentException
    {
        // Invalid slot (including the -999 outside rawslot constant)
        if (rawSlot < 0)
        {
            return null;
        }

        // Debug:
        // System.out.println("InventoryLocation: " + rawSlot + " | " + inventoryType);
        switch (inventoryType)
        {
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

                final double extraYChest = rawSlot < 54 ?
                                           // Chest slot or player inv?
                                           (rawSlot < 27 ? 0 : 0.5D)
                                           // Quickbar
                                                        : 0.75D;

                return new double[]{
                        rawSlot % 9,
                        // + 0.5D as of the partition in the middle of the inv.
                        ((rawSlot / 9) + extraYChest)
                };
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
                if (rawSlot < 9)
                {
                    return new double[]{
                            4 + rawSlot % 3,
                            (rawSlot / 3)
                    };
                }

                final double extraYDispenser = rawSlot < 36 ? 2.5D : 2.75D;

                return new double[]{
                        rawSlot % 9,
                        // 3.5D is the normal y - offset, but rawslots begin over 9 thus
                        // the Math.floor would need a subtraction by 1, thus 2.5D.
                        extraYDispenser + (rawSlot / 9)
                };
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
                switch (rawSlot)
                {
                    case 0:
                        return new double[]{
                                2.5F,
                                0F
                        };
                    case 1:
                        return new double[]{
                                2.5F,
                                2F
                        };
                    case 2:
                        return new double[]{
                                6F,
                                1F
                        };
                    default:
                        final double extraYFurnace = rawSlot < 30 ? 3.5D : 3.75D;
                        return new double[]{
                                (rawSlot - 3) % 9,
                                // 3.5D is the normal y - offset, and rawslots begin below 9
                                // thus it is ok to use 3.5D here.
                                extraYFurnace + ((rawSlot - 3) / 9)
                        };
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

                switch (rawSlot)
                {
                    case 0:
                        return new double[]{
                                6.5D,
                                1D
                        };

                    default:
                        if (rawSlot <= 9)
                        {
                            int xTemp = rawSlot % 3;
                            float yTemp = rawSlot / 3F;
                            return new double[]{
                                    (xTemp == 0 ? 3 : xTemp) + 0.25D,
                                    yTemp <= 1 ? 0 : (yTemp <= 2 ? 1 : 2)
                            };
                        }
                        else
                        {
                            final double extraYWorkbench = rawSlot < 37 ? 2.5D : 2.75D;
                            return new double[]{
                                    (rawSlot - 1) % 9,
                                    // 3.5D is the normal y - offset, but rawslots begin over 9 thus
                                    // the Math.floor would need a subtraction by 1, thus 2.5D.
                                    extraYWorkbench + ((rawSlot - 1) / 9)
                            };
                        }
                }

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
                if (rawSlot <= 4)
                {
                    return new double[]{
                            2D + rawSlot,
                            1D
                    };
                }

                rawSlot -= 5;
                final double extraYHopper = rawSlot < 32 ? 2.5D : 2.75D;
                return new double[]{
                        rawSlot % 9,
                        (extraYHopper + (rawSlot / 9))
                };

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
                if (rawSlot == 0)
                {
                    return new double[]{
                            7.5D,
                            1.5D
                    };
                }

                // Crafting slots
                if (rawSlot <= 4)
                {
                    return new double[]{
                            5.5D - (rawSlot % 2),
                            rawSlot <= 2 ? 1D : 2D
                    };
                }

                // Armor slots
                if (rawSlot <= 8)
                {
                    return new double[]{
                            0D,
                            (rawSlot - 5)
                    };
                }

                // + 0.25D as of the partition in the middle of the inv.
                // another + 0.25D as of the partition between the inv and the quickbar.
                final double extraYPlayer = rawSlot < 36 ? 4.25D : 4.5D;

                // Normal slots
                rawSlot -= 9;
                return new double[]{
                        rawSlot % 9,
                        ((rawSlot / 9) + extraYPlayer)
                };
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
     * This schedules an {@link org.bukkit.inventory.Inventory} update to be executed synchronously in the next server tick
     *
     * @param player the {@link Player} who's {@link org.bukkit.inventory.Inventory} should be updated.
     */
    public static void syncUpdateInventory(Player player)
    {
        Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), player::updateInventory);
    }
}

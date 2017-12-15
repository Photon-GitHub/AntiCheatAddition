package de.photon.AACAdditionPro.util.inventory;

import de.photon.AACAdditionPro.AACAdditionPro;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;

public final class InventoryUtils
{
    /**
     * Calculates the distance between two vectors in n-dimensional space.
     */
    public static double vectorDistance(double[] firstVector, double[] secondVector)
    {
        if (firstVector.length != secondVector.length)
        {
            throw new IllegalArgumentException("Vectors have not the same amount of dimensions.");
        }

        // float is sufficient here as the data will not be based on very accurate numbers.
        double sum = 0;
        for (int i = 0; i < firstVector.length; i++)
        {
            sum += Math.pow(secondVector[i] - firstVector[i], 2);
        }

        return Math.sqrt(sum);
    }

    /**
     * Used to locate a slot in an {@link org.bukkit.inventory.Inventory}.
     * The coordinate-system is (0|0) in upper-left corner.
     * <br>
     * Please make sure that the {@link Player} is not riding a horse, as this method does not support horses/donkeys/mules
     *
     * @param rawSlot       the number that is returned by getRawSlot()
     * @param inventoryType the inventory layout when
     *
     * @return the coords of a slot or null if it is invalid.
     */
    public static double[] locateSlot(int rawSlot, InventoryType inventoryType, InventoryType.SlotType slotType)
    {
        System.out.println(rawSlot + " | " + inventoryType);
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
                return new double[]{
                        rawSlot % 9,
                        rawSlot / 9
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
                            rawSlot / 3
                    };
                }

                return new double[]{
                        rawSlot % 9,
                        // It is 3.5 away, and / 9 will always result in something >= 1
                        2.5F + rawSlot / 9
                };
            case FURNACE:
                /*
                 *                 0
                 *
                 *                                    2
                 *
                 *                 1
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
                                3.5F,
                                0F
                        };
                    case 1:
                        return new double[]{
                                3.5F,
                                2F
                        };
                    case 2:
                        return new double[]{
                                7F,
                                1F
                        };
                    default:
                        return new double[]{
                                (rawSlot - 3) % 9,
                                // It is 3.5 away, and (x - 3) / 9 will result in something >= 0
                                3.5F + (rawSlot - 3) / 9
                        };
                }
            case WORKBENCH:
                /*
                 *          1       2       3
                 *
                 *          4       5       6            0
                 *
                 *          7       8       9
                 * ------------------------------------------------------
                 * ------------------------------------------------------
                 * 10                         -                      18
                 *
                 * 19                        -                       27
                 *
                 * 28                        -                       36
                 * ------------------------------------------------------
                 * 37                        -                       45
                 */
                break;
            case CRAFTING:
                /* Player Inventory
                 * 5
                 *
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
                break;
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
                break;
            // TODO: is this needed or even correct?
            case PLAYER:
                if (slotType != null)
                {
                    switch (slotType)
                    {
                        // Y = 0
                        case QUICKBAR:
                            return new double[]{
                                    rawSlot % 9, 0
                            };

                        case CRAFTING:
                            return new double[]
                                    {
                                            // 80 and 82
                                            rawSlot % 2 == 0 ?
                                            5.5F :
                                            6.5F,
                                            // 82 and 83
                                            rawSlot > 81 ?
                                            6.5F :
                                            7.5F
                                    };

                        case RESULT:
                            return new double[]{
                                    7.5F,
                                    7
                            };

                        case ARMOR:
                            break;
                        case CONTAINER:
                            break;
                        case FUEL:
                            break;
                        default:
                            break;
                    }
                }

                // cases: OUTSIDE
                return null;
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

package de.photon.aacadditionpro.util.inventory

import com.google.common.collect.ImmutableList
import de.photon.aacadditionpro.AACAdditionPro
import de.photon.aacadditionpro.ServerVersion
import de.photon.aacadditionpro.util.mathematics.MathUtil
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object InventoryUtil {
    /**
     * Gets the content of the main hand for version 1.8.8 or the content of both hands in higher versions.
     *
     * @return an [ImmutableList] which contains all the [ItemStack]s a player has: 1 in MC 1.8.8 and 2 in higher versions.
     */
    @JvmStatic
    fun getHandContents(player: Player): List<ItemStack> {
        return if (ServerVersion.getActiveServerVersion() == ServerVersion.MC18) ImmutableList.of(player.inventory.itemInHand)
        else ImmutableList.of(player.inventory.itemInMainHand, player.inventory.itemInOffHand)
    }

    /**
     * Checks if an [Inventory] is empty.
     */
    @JvmStatic
    fun isInventoryEmpty(inventory: Inventory): Boolean {
        return inventory.contents.all { itemStack -> itemStack == null }
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
    @JvmStatic
    fun distanceBetweenSlots(rawSlotOne: Int, rawSlotTwo: Int, inventoryType: InventoryType?): Double {
        val first = locateSlot(rawSlotOne, inventoryType)
        val second = locateSlot(rawSlotTwo, inventoryType)
        return if (first == null || second == null) -1.0 else first.distance(second)
    }

    /**
     * Used to locate a slot in an [Inventory].
     * The coordinate-system is (0|0) in upper-left corner.
     * <br></br>
     * Please make sure that the [Player] is not riding a horse, as this method does not support horses/donkeys/mules
     *
     * @param rawSlot       the number that is returned by getRawSlot()
     * @param inventoryType the inventory layout when the click happened.
     *
     * @return the coordinates of a slot or null if it is invalid.
     */
    @JvmStatic
    @Throws(IllegalArgumentException::class)
    fun locateSlot(rawSlot: Int, inventoryType: InventoryType?): SlotLocation? {
        // Invalid slot (including the -999 outside rawslot constant)
        if (rawSlot < 0) return null
        val x: Double
        var y: Double
        when (inventoryType) {
            InventoryType.CHEST, InventoryType.ENDER_CHEST -> {
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
                x = (rawSlot % 9).toDouble()
                y = (rawSlot / 9).toDouble()
                if (rawSlot >= 27) y += 0.5 // Player inventory
                if (rawSlot >= 54) y += 0.25 // Quickbar
                return SlotLocation(x, y)
            }
            InventoryType.DISPENSER, InventoryType.DROPPER -> {
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
                if (rawSlot < 9) return SlotLocation((4 + rawSlot % 3).toDouble(), (rawSlot / 3).toDouble())
                x = (rawSlot % 9).toDouble()
                // 3.5D is normal offset, but the rawslots in the player inventory start with 9, which will
                // automatically add 1 in the division -> offset 2.5.
                y = rawSlot / 9 + 2.5
                if (rawSlot >= 36) y += 0.25 // Quickbar
                return SlotLocation(x, y)
            }
            InventoryType.FURNACE -> return when (rawSlot) {
                0 -> SlotLocation(2.5, 0.0)
                1 -> SlotLocation(2.5, 2.0)
                2 -> SlotLocation(6.0, 1.0)
                else -> {
                    x = ((rawSlot - 3) % 9).toDouble()
                    y = (rawSlot - 3) / 9 + 3.5
                    if (rawSlot >= 30) y += 0.25 // Quickbar
                    SlotLocation(x, y)
                }
            }
            InventoryType.WORKBENCH -> {
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
                if (rawSlot == 0) return SlotLocation(6.5, 1.0)
                if (rawSlot <= 9) {
                    x = (rawSlot - 1) % 3 + 1.25
                    y = ((rawSlot - 1) / 3).toDouble()
                    return SlotLocation(x, y)
                }
                x = ((rawSlot - 1) % 9).toDouble()
                y = (rawSlot - 1) / 9 + 2.5
                if (rawSlot >= 37) y += 0.25 // Quickbar
                return SlotLocation(x, y)
            }
            InventoryType.ENCHANTING -> {
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
                return null
            }
            InventoryType.MERCHANT -> {
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
                return null
            }
            InventoryType.ANVIL -> {
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
                return null
            }
            InventoryType.BEACON -> {
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
                return null
            }
            InventoryType.HOPPER -> {
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
                if (rawSlot <= 4) return SlotLocation(2.0 + rawSlot, 1.0)
                x = ((rawSlot - 5) % 9).toDouble()
                y = (rawSlot - 5) / 9 + 2.5
                if (rawSlot >= 32) y += 0.25 // Quickbar
                return SlotLocation(x, y)
            }
            InventoryType.CRAFTING, InventoryType.PLAYER -> {
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
                if (rawSlot == 0) return SlotLocation(7.5, 1.5)

                // Crafting slots
                if (rawSlot <= 4) return SlotLocation(5.5 - rawSlot % 2, if (rawSlot <= 2) 1.0 else 2.0)

                // Armor slots
                if (rawSlot <= 8) return SlotLocation(0.0, (rawSlot - 5).toDouble())
                x = ((rawSlot - 9) % 9).toDouble()
                y = (rawSlot - 9) / 9 + 4.25
                if (rawSlot >= 36) y += 0.25 // Quickbar
                return SlotLocation(x, y)
            }
            // CREATIVE (false positives), PLAYER, SHULKER_BOX, BREWING_STAND (version compatibility)
            InventoryType.CREATIVE, InventoryType.SHULKER_BOX -> return null
            else -> return null
        }
    }

    /**
     * This schedules an [Inventory] update to be executed synchronously in the next server tick
     *
     * @param player the [Player] who's [Inventory] should be updated.
     */
    @JvmStatic
    fun syncUpdateInventory(player: Player?) {
        Bukkit.getScheduler().runTask(AACAdditionPro.getInstance(), Runnable { player?.updateInventory() })
    }

    data class SlotLocation(val x: Double, val y: Double) {
        fun distance(other: SlotLocation): Double {
            return MathUtil.fastHypot(x - other.x, y - other.y)
        }
    }
}
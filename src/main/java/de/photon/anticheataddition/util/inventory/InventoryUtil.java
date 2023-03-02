package de.photon.anticheataddition.util.inventory;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public sealed interface InventoryUtil permits LegacyInventoryUtil, ModernInventoryUtil
{
    InventoryUtil INSTANCE = ServerVersion.is18() ? new LegacyInventoryUtil() : new ModernInventoryUtil();

    /**
     * Helper method for locateSlot.
     * This calculates a slot location in the lower (player inventory) part of the screen, as that is always the same, but with different rawslot offsets.
     *
     * @param startSlot the first rawSlot in the lower (player inventory) part of the screen.
     * @param rawSlot   the rawSlot that should be located.
     */
    static Optional<SlotLocation> lowerInventoryLocation(int startSlot, int rawSlot)
    {
        final int normalizedSlot = rawSlot - startSlot;
        final int x = normalizedSlot % 9;
        // 3.5 is the normal (tested) distance.
        final int row = normalizedSlot / 9;
        double y = row + 3.5D;

        // Hotbar handling.
        if (normalizedSlot >= 27) y += 0.25D;
        return SlotLocation.opOf(x, y);
    }

    /**
     * Checks if an {@link Inventory} is empty.
     */
    static boolean isInventoryEmpty(Inventory inventory)
    {
        for (ItemStack content : inventory.getContents()) {
            if (content != null) return false;
        }
        return true;
    }

    /**
     * This schedules an {@link Inventory} update to be executed synchronously in the next server tick
     *
     * @param player the {@link Player} who's {@link Inventory} should be updated.
     */
    static void syncUpdateInventory(Player player)
    {
        Bukkit.getScheduler().runTask(AntiCheatAddition.getInstance(), player::updateInventory);
    }

    /**
     * Gets the content of the main hand for version 1.8.8 or the content of both hands in higher versions.
     *
     * @return an {@link List} which contains all the {@link ItemStack}s a player has: 1 in MC 1.8.8 and 2 in higher versions.
     */
    List<ItemStack> getHandContents(final Player player);

    /**
     * Calculates the distance between two raw slots.
     *
     * @param rawSlotOne the first (raw) slot
     * @param rawSlotTwo the second (raw) slot
     * @param inventory  the inventory where the click happened.
     *
     * @return the distance between the two slots or -1 if locating the slots failed.
     */
    static OptionalDouble distanceBetweenSlots(final int rawSlotOne, final int rawSlotTwo, @NotNull final Inventory inventory)
    {
        final var first = INSTANCE.locateSlot(rawSlotOne, inventory);
        final var second = INSTANCE.locateSlot(rawSlotTwo, inventory);

        return first.isPresent() && second.isPresent() ? OptionalDouble.of(first.get().distance(second.get())) : OptionalDouble.empty();
    }

    /**
     * Used to locate a slot in an {@link Inventory}.
     * The coordinate-system is (0|0) in upper-left corner.
     * <br>
     * Please make sure that the {@link Player} is not riding a horse, as this method does not support horses/donkeys/mules
     *
     * @param rawSlot   the number that is returned by getRawSlot()
     * @param inventory the inventory where the click happened.
     *
     * @return the coordinates of a slot or null if it is invalid.
     *
     * @see <a href="https://wiki.vg/Inventory">https://wiki.vg/Inventory</a>
     */
    Optional<SlotLocation> locateSlot(int rawSlot, final Inventory inventory) throws IllegalArgumentException;

    record SlotLocation(double x, double y)
    {
        public static final SlotLocation DUMMY = new SlotLocation(0, 0);

        static Optional<SlotLocation> opOf(double x, double y)
        {
            return Optional.of(new SlotLocation(x, y));
        }

        public double distance(SlotLocation other)
        {
            return MathUtil.fastHypot(x - other.x, y - other.y);
        }
    }
}

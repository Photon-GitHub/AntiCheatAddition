package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.mathematics.TimeUtil;
import de.photon.anticheataddition.util.minecraft.ping.PingProvider;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.OptionalDouble;

public final class InventoryMultiInteraction extends ViolationModule implements Listener
{
    public static final InventoryMultiInteraction INSTANCE = new InventoryMultiInteraction();

    private final int cancelVl = loadInt(".cancel_vl", 25);

    private InventoryMultiInteraction()
    {
        super("Inventory.parts.MultiInteraction");
    }


    /**
     * Calculates the distance between the last clicked slot and the current clicked slot.
     *
     * @param user  The user performing the click.
     * @param event The inventory click event.
     *
     * @return An OptionalDouble containing the distance if available, or empty if the inventory is null.
     */
    private static OptionalDouble distanceToLastClickedSlot(User user, InventoryClickEvent event)
    {
        final var inventory = event.getClickedInventory();
        return inventory == null ? OptionalDouble.empty() : InventoryUtil.distanceBetweenSlots(event.getRawSlot(), user.getData().number.lastRawSlotClicked, inventory);
    }

    /**
     * Determines if the distance to the last clicked slot is small.
     *
     * @param user  The user performing the click.
     * @param event The inventory click event.
     *
     * @return True if the distance is less than 4, false otherwise.
     */
    private static boolean smallDistance(User user, InventoryClickEvent event)
    {
        return distanceToLastClickedSlot(user, event).orElse(0D) < 4;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event)
    {
        final var user = User.getUser(event.getWhoClicked().getUniqueId());
        if (user == null) return;

        Log.finer(() -> "Inventory-Debug | Player: " + user.getPlayer().getName() + " | MultiInteraction assumptions | Invalid: " + User.isUserInvalid(user, this) +
                        ", null inv: " + (event.getClickedInventory() == null) +
                        ", Adventure/Survival: " + !user.inAdventureOrSurvivalMode() +
                        ", MinTPS: " + !Inventory.hasMinTPS() +
                        ", MaxPing: " + !PingProvider.INSTANCE.atMostMaxPing(user.getPlayer(), Inventory.INSTANCE.getMaxPing()) +
                        ", Last slot: " + (event.getRawSlot() == user.getData().number.lastRawSlotClicked));

        if (User.isUserInvalid(user, this) ||
            event.getClickedInventory() == null ||
            // Creative-clear might trigger this.
            !user.inAdventureOrSurvivalMode() ||
            // Minimum TPS before the check is activated as of a huge amount of fps
            !Inventory.hasMinTPS() ||
            // Maximum ping
            !PingProvider.INSTANCE.atMostMaxPing(user.getPlayer(), Inventory.INSTANCE.getMaxPing()) ||
            // False positive: Click-spamming on the same slot
            event.getRawSlot() == user.getData().number.lastRawSlotClicked) return;

        // Default vl is 6
        int addedVl = 6;
        // Time in ticks that have to pass to not be flagged by this check for too fast inventory interactions.
        final int enforcedTicks;

        switch (event.getAction()) {
            // ------------------------------------------ Exemptions -------------------------------------------- //
            // Nothing happens, therefore exempted
            // Unknown reason might not be safe to handle
            // False positive with collecting all items of one type in the inventory
            // False positives due to auto-dropping feature of minecraft when holding q
            case NOTHING, UNKNOWN, COLLECT_TO_CURSOR, DROP_ALL_SLOT, DROP_ONE_SLOT:
                return;
            // ------------------------------------------ Normal -------------------------------------------- //
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD:
                addedVl = 1;
                enforcedTicks = 1;

                // Too small distance to check correctly.
                if (smallDistance(user, event)) return;
                break;

            case PICKUP_ALL, PICKUP_SOME, PICKUP_HALF, PICKUP_ONE, PLACE_ALL, PLACE_SOME, PLACE_ONE:
                // No false positives to check for.
                addedVl = 8;
                enforcedTicks = smallDistance(user, event) ? 1 : 5;
                break;

            case DROP_ALL_CURSOR, DROP_ONE_CURSOR, CLONE_STACK:
                // No false positives to check for.
                enforcedTicks = 4;
                break;

            case MOVE_TO_OTHER_INVENTORY:
                // Last material false positive due to the fast move all items shortcut.
                if (event.getCurrentItem() == null || user.getData().object.lastMaterialClicked == event.getCurrentItem().getType()) return;

                // Depending on the distance of the clicks.
                enforcedTicks = smallDistance(user, event) ? 1 : 2;
                break;

            case SWAP_WITH_CURSOR:
                enforcedTicks = switch (event.getSlotType()) {
                    // Armor slots are not eligible for fewer ticks as of quick change problems with the feet slot.
                    // No false positives possible in fuel or crafting slot as it is only one slot which is separated from others
                    case FUEL, RESULT -> 4;

                    // Default tested value.
                    default -> 2;
                };
                break;

            default:
                enforcedTicks = 0;
                break;
        }

        Log.finer(() -> "Inventory-Debug | Player: %s | MultiInteraction delays | Enforced: %d, passed: %d".formatted(user.getPlayer().getName(), TimeUtil.toMillis(enforcedTicks), user.getTimeMap().at(TimeKey.INVENTORY_CLICK).passedTime()));

        // Convert ticks to millis.
        // 25 to account for server lag.
        if (user.hasClickedInventoryRecently(25L + TimeUtil.toMillis(enforcedTicks))) {
            this.getManagement().flag(Flag.of(user).setAddedVl(addedVl).setCancelAction(cancelVl, () -> {
                event.setCancelled(true);
                InventoryUtil.syncUpdateInventory(user.getPlayer());
            }).setDebug(() -> "Inventory-Debug | Player: %s moved items too quickly.".formatted(user.getPlayer().getName())));
        }
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(100, 1).build();
    }
}

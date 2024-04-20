package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.messaging.Log;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class InventoryFrequency extends ViolationModule implements Listener
{
    private static final long OPEN_CLOSE_TIME = InventoryMove.BASE_BREAKING_TIME + 50L;
    public static final InventoryFrequency INSTANCE = new InventoryFrequency();

    private InventoryFrequency()
    {
        super("Inventory.parts.Frequency");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(final InventoryCloseEvent event)
    {
        final var user = User.getUser(event.getPlayer().getUniqueId());
        if (User.isUserInvalid(user, this) ||
            // Creative-clear might trigger this.
            !user.inAdventureOrSurvivalMode() ||
            // Minimum TPS before the check is activated as of a huge amount of fps
            !Inventory.hasMinTPS()) return;

        final long passedTime = user.getTimeMap().at(TimeKey.INVENTORY_OPENED).passedTime();
        Log.finer(() -> "Inventory-Debug | Player: " + user.getPlayer().getName() + " frequency passed time: " + passedTime);

        if (user.getData().counter.inventoryFrequencyFails.conditionallyIncDec(passedTime <= OPEN_CLOSE_TIME)) {
            this.getManagement().flag(Flag.of(user)
                                          .setAddedVl(3)
                                          .setDebug(() -> "Inventory-Debug | Player: " + user.getPlayer().getName() + " quickly opens and closes inventories for prolonged periods of time (D: " + passedTime + ')'));
        }
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(400, 1).build();
    }
}

package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public final class InventoryRotation extends ViolationModule implements Listener
{
    public static final InventoryRotation INSTANCE = new InventoryRotation();

    private InventoryRotation()
    {
        super("Inventory.parts.Rotation");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerMove(PlayerMoveEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this) || event.getTo() == null) return;

        // Not flying (may trigger some fps)
        if (Inventory.hasMinTPS() &&
            !user.getPlayer().getAllowFlight() &&
            // Player is in an inventory
            user.hasOpenInventory() &&
            // Head-Rotation has changed (detection)
            (event.getFrom().getYaw() != event.getTo().getYaw() ||
             event.getFrom().getPitch() != event.getTo().getPitch()) &&
            // No recently tp
            !Inventory.teleportOrWorldChangeBypassed(user) &&
            // The player has opened his inventory for at least one second.
            user.notRecentlyOpenedInventory(1000))
        {
            getManagement().flag(Flag.of(user).setDebug(() -> "Inventory-Debug | Player: " + user.getPlayer().getName() + " sent new rotations while having an open inventory."));
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

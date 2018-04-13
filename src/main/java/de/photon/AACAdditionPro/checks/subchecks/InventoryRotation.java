package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class InventoryRotation implements Listener, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 50L);

    @EventHandler
    public void on(final PlayerMoveEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user))
        {
            return;
        }

        // Not flying (may trigger some fps)
        if (!user.getPlayer().getAllowFlight() &&
            // Head-Rotation has changed (detection)
            (event.getFrom().getPitch() != event.getTo().getPitch() || event.getFrom().getYaw() != event.getTo().getYaw()) &&
            // No recently tp
            !user.getTeleportData().recentlyUpdated(0, 1000) &&
            // Player is in an inventory
            user.getInventoryData().hasOpenInventory() &&
            // The player has opened his inventory for at least one second.
            user.getInventoryData().notRecentlyOpened(1000))
        {
            vlManager.flag(event.getPlayer(), -1, () -> {}, () -> {});
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.INVENTORY_ROTATION;
    }
}
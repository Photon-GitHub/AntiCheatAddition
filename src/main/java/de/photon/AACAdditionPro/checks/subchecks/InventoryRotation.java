package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class InventoryRotation implements Listener, AACAdditionProCheck
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 50L);

    @EventHandler
    public void on(final PlayerMoveEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (AACAdditionProCheck.isUserInvalid(user)) {
            return;
        }

        // Not flying (may trigger some fps)
        if (!user.getPlayer().getAllowFlight() &&
            // Head-Rotation has changed (detection)
            (event.getFrom().getPitch() != event.getTo().getPitch() || event.getFrom().getYaw() != event.getTo().getYaw()) &&
            // No recently tp
            !user.getTeleportData().recentlyUpdated(1000) &&
            // Player is in an inventory
            user.getInventoryData().hasOpenInventory() &&
            // Was already in inventory
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
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.INVENTORY_ROTATION;
    }
}
package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.minecraft.entity.EntityUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class InventorySprinting extends ViolationModule implements Listener
{
    public static final InventorySprinting INSTANCE = new InventorySprinting();

    private final int cancelVl = loadInt(".cancel_vl", 110);

    private InventorySprinting()
    {
        super("Inventory.parts.Sprinting");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event)
    {
        val user = User.getUser(event.getWhoClicked().getUniqueId());
        if (User.isUserInvalid(user, this)) return;

        // Flight may trigger this
        if (!user.getPlayer().getAllowFlight() &&
            // Not using an Elytra
            !EntityUtil.INSTANCE.isFlyingWithElytra(user.getPlayer()) &&
            // Sprinting and Sneaking as detection
            (user.getPlayer().isSprinting() || user.getPlayer().isSneaking()) &&
            // The player opened the inventory at least a quarter second ago
            user.notRecentlyOpenedInventory(250) &&
            // Is the player moving
            user.hasMovedRecently(TimeKey.LAST_HEAD_OR_OTHER_MOVEMENT, 1000))
        {
            this.getManagement().flag(Flag.of(user)
                                          .setAddedVl(30)
                                          .setCancelAction(this.cancelVl, () -> {
                                              event.setCancelled(true);
                                              InventoryUtil.syncUpdateInventory(user.getPlayer());
                                          })
                                          .setDebug("Inventory-Debug | Player: " + user.getPlayer().getName() + " interacted with an inventory while sprinting or sneaking."));
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

package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.inventory.InventoryUtil;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import de.photon.aacadditionpro.util.world.EntityUtil;
import lombok.Getter;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

class InventorySprinting extends ViolationModule implements Listener
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private int cancelVl;

    public InventorySprinting()
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
            !EntityUtil.isFlyingWithElytra(user.getPlayer()) &&
            // Sprinting and Sneaking as detection
            (user.getPlayer().isSprinting() || user.getPlayer().isSneaking()) &&
            // The player opened the inventory at least a quarter second ago
            user.notRecentlyOpenedInventory(250) &&
            // Is the player moving
            user.hasMovedRecently(TimestampKey.LAST_HEAD_OR_OTHER_MOVEMENT, 1000))
        {
            this.getManagement().flag(Flag.of(user)
                                          .setAddedVl(20)
                                          .setCancelAction(this.cancelVl, () -> {
                                              event.setCancelled(true);
                                              InventoryUtil.syncUpdateInventory(user.getPlayer());
                                          })
                                          .setEventNotCancelledAction(() -> DebugSender.getInstance().sendDebug("Inventory-Verbose | Player: " + user.getPlayer().getName() + " interacted with an inventory while sprinting or sneaking.")));
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return new ViolationLevelManagement(this, 100L, 1);
    }
}

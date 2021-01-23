package de.photon.aacadditionproold.modules.checks.inventory;

import de.photon.aacadditionproold.modules.ListenerModule;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.user.TimestampKey;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.UserManager;
import de.photon.aacadditionproold.util.entity.EntityUtil;
import de.photon.aacadditionproold.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionproold.util.inventory.InventoryUtils;
import de.photon.aacadditionproold.util.messaging.VerboseSender;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;

class SprintingPattern implements ListenerModule
{
    @Getter
    private static final SprintingPattern instance = new SprintingPattern();

    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private int cancelVl;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event)
    {
        final User user = UserManager.getUser(event.getWhoClicked().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

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
            Inventory.getInstance().getViolationLevelManagement().flag(user.getPlayer(), 20, this.cancelVl,
                                                                       () -> {
                                                                           event.setCancelled(true);
                                                                           InventoryUtils.syncUpdateInventory(user.getPlayer());
                                                                       },
                                                                       () -> VerboseSender.getInstance().sendVerboseMessage("Inventory-Verbose | Player: " + user.getPlayer().getName() + " interacted with an inventory while sprinting or sneaking."));
        }
    }

    @Override
    public boolean isSubModule()
    {
        return true;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.Sprinting";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.INVENTORY;
    }
}

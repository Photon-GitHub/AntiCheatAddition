package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.entity.EntityUtil;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.inventory.InventoryUtils;
import lombok.Getter;
import org.bukkit.event.inventory.InventoryClickEvent;

class SprintingPattern extends PatternModule.Pattern<User, InventoryClickEvent>
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private int cancelVl;

    @Override
    protected int process(User user, InventoryClickEvent event)
    {
        // Flight may trigger this
        if (!user.getPlayer().getAllowFlight() &&
            // Not using an Elytra
            !EntityUtil.isFlyingWithElytra(user.getPlayer()) &&
            // Sprinting and Sneaking as detection
            (user.getPlayer().isSprinting() || user.getPlayer().isSneaking()) &&
            // The player opened the inventory at least a quarter second ago
            user.notRecentlyOpenedInventory(250) &&
            // Is the player moving
            user.hasPlayerMovedRecently(TimestampKey.LAST_HEAD_OR_OTHER_MOVEMENT, 1000))
        {
            message = "Inventory-Verbose | Player: " + user.getPlayer().getName() + " interacted with an inventory while sprinting or sneaking.";
            return 20;
        }
        return 0;
    }

    @Override
    public void cancelAction(User user, InventoryClickEvent event)
    {
        event.setCancelled(true);
        InventoryUtils.syncUpdateInventory(user.getPlayer());
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

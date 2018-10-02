package de.photon.AACAdditionPro.modules.checks.inventory;

import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.data.PositionData;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.entity.EntityUtil;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import lombok.Getter;
import org.bukkit.event.inventory.InventoryClickEvent;

class SprintingPattern extends PatternModule.Pattern<User, InventoryClickEvent>
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private static int cancelVl;

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
            user.getInventoryData().notRecentlyOpened(250) &&
            // Is the player moving
            user.getPositionData().hasPlayerMovedRecently(1000, PositionData.MovementType.ANY))
        {
            VerboseSender.getInstance().sendVerboseMessage("Inventory-Verbose | Player: " + user.getPlayer().getName() + " interacted with an inventory while sprinting or sneaking.");
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

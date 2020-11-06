package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.inventory.InventoryUtils;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import de.photon.aacadditionpro.util.server.ServerUtil;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class PerfectExitPattern implements ListenerModule
{
    @Getter
    private static final PerfectExitPattern instance = new PerfectExitPattern();

    @LoadFromConfiguration(configPath = ".violation_threshold")
    private int violationThreshold;

    @LoadFromConfiguration(configPath = ".min_tps")
    private double minTps;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(final InventoryCloseEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // Creative-clear might trigger this.
        if (user.inAdventureOrSurvivalMode() &&
            // Minimum TPS before the check is activated as of a huge amount of fps
            ServerUtil.getTPS() > minTps &&
            // Inventory is empty
            InventoryUtils.isInventoryEmpty(event.getInventory()))
        {
            final long passedTime = user.getTimestampMap().passedTime(TimestampKey.LAST_INVENTORY_CLICK_ON_ITEM);
            if (passedTime <= 70) {
                if (++user.getInventoryData().perfectExitFails >= this.violationThreshold) {
                    Inventory.getInstance().getViolationLevelManagement().flag(user.getPlayer(),
                                                                               passedTime <= 50 ? 15 : 7,
                                                                               -1, () -> {},
                                                                               () -> VerboseSender.getInstance().sendVerboseMessage("Inventory-Verbose | Player: " + user.getPlayer().getName() + " exits inventories in a bot-like way (D: " + passedTime + ')'));
                }
            } else if (user.getInventoryData().perfectExitFails > 0) {
                --user.getInventoryData().perfectExitFails;
            }
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
        return this.getModuleType().getConfigString() + ".parts.PerfectExit";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.INVENTORY;
    }
}

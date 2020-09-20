package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.modules.BatchProcessorModule;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.user.subdata.datawrappers.InventoryClick;
import de.photon.aacadditionpro.util.datastructures.batch.BatchProcessor;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.server.ServerUtil;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class AverageHeuristicPattern implements BatchProcessorModule<InventoryClick>, ListenerModule
{
    @Getter
    private static final AverageHeuristicPattern instance = new AverageHeuristicPattern();

    @LoadFromConfiguration(configPath = ".max_ping")
    private double maxPing;
    @LoadFromConfiguration(configPath = ".min_tps")
    private double minTps;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event)
    {
        final User user = UserManager.getUser(event.getWhoClicked().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // Make sure that we have normal click actions.
        if ((event.getClick() == ClickType.DROP || event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) &&
            // Creative-clear might trigger this.
            user.inAdventureOrSurvivalMode() &&
            // Minimum TPS before the check is activated as of a huge amount of fps
            ServerUtil.getTPS() > minTps &&
            // Minimum ping
            (maxPing < 0 || ServerUtil.getPing(user.getPlayer()) <= maxPing))
        {
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                user.getInventoryData().averageHeuristicMisclicks++;
            }
            // Shift - Double - Click shortcut will generate a lot of clicks.
            else if (user.getDataMap().getValue(DataKey.LAST_MATERIAL_CLICKED) != event.getCurrentItem().getType()) {
                user.getInventoryData().getBatch().addDataPoint(InventoryClick.fromClickEvent(event));
            }
        }
    }

    @Override
    public BatchProcessor<InventoryClick> getBatchProcessor()
    {
        return AverageHeuristicBatchProcessor.getInstance();
    }

    @Override
    public boolean isSubModule()
    {
        return true;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.AverageHeuristic";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.INVENTORY;
    }
}

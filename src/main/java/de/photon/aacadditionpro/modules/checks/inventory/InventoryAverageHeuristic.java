package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.user.data.batch.InventoryBatch;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.server.PingProvider;
import de.photon.aacadditionpro.util.server.TPSProvider;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.Getter;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

public class InventoryAverageHeuristic extends ViolationModule implements Listener
{
    @Getter
    private static final InventoryAverageHeuristic instance = new InventoryAverageHeuristic();

    @LoadFromConfiguration(configPath = ".max_ping")
    private double maxPing;
    @LoadFromConfiguration(configPath = ".min_tps")
    private double minTps;

    public InventoryAverageHeuristic()
    {
        super("Inventory.parts.AverageHeuristic");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event)
    {
        val user = User.getUser(event.getWhoClicked().getUniqueId());
        if (User.isUserInvalid(user, this)) return;

        // Make sure that we have normal click actions.
        if ((event.getClick() == ClickType.DROP || event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.LEFT || event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT) &&
            // Creative-clear might trigger this.
            user.inAdventureOrSurvivalMode() &&
            // Minimum TPS before the check is activated as of a huge amount of fps
            TPSProvider.getTPS() > minTps &&
            // Minimum ping
            (maxPing < 0 || PingProvider.getPing(user.getPlayer()) <= maxPing))
        {
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) user.getDataMap().getCounter(DataKey.CounterKey.INVENTORY_AVERAGE_HEURISTICS_MISCLICKS).increment();
                // Shift - Double - Click shortcut will generate a lot of clicks.
            else if (user.getDataMap().getObject(DataKey.ObjectKey.LAST_MATERIAL_CLICKED) != event.getCurrentItem().getType())
                user.getInventoryBatch().addDataPoint(InventoryBatch.InventoryClick.fromClickEvent(event));
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val batchProcessor = new AverageHeuristicBatchProcessor(this);
        return ModuleLoader.builder(this)
                           .batchProcessor(batchProcessor)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(160, 2).build();
    }
}

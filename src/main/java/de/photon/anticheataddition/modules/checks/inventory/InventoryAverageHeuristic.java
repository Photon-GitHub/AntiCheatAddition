package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.modules.checks.inventory.ClickTypes.*;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.batch.InventoryBatch;
import de.photon.anticheataddition.util.minecraft.ping.PingProvider;
import de.photon.anticheataddition.util.minecraft.tps.TPSProvider;
import de.photon.anticheataddition.util.minecraft.world.MaterialUtil;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Set;

public final class InventoryAverageHeuristic extends ViolationModule implements Listener
{
    public static final InventoryAverageHeuristic INSTANCE = new InventoryAverageHeuristic();

    private final Set<SupportedClickType> supportedClickTypes = Set.of(new DropClickType(), new RightClickType(), new LeftClickType(), new ShiftLeftClickType(), new ShiftRightClickType());

    private final int maxPing = loadInt(".max_ping", 400);
    private final double minTps = loadDouble(".min_tps", 15.5);

    private InventoryAverageHeuristic()
    {
        super("Inventory.parts.AverageHeuristic");
    }

    private boolean supportedClickType(ClickType clickType) {
        return supportedClickTypes.stream().anyMatch(supportedClickType -> supportedClickType.isSupported(clickType));
    }


    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(InventoryClickEvent event)
    {
        final var user = User.getUser(event.getWhoClicked().getUniqueId());
        if (User.isUserInvalid(user, this)) return;

        // Make sure that we have normal click actions.
        if (supportedClickType(event.getClick()) &&
            // Creative-clear might trigger this.
            user.inAdventureOrSurvivalMode() &&
            // Minimum TPS before the check is activated as of a huge amount of fps
            TPSProvider.INSTANCE.atLeastTPS(minTps) &&
            // Minimum ping
            PingProvider.INSTANCE.atMostMaxPing(user.getPlayer(), maxPing))
        {
            if (event.getCurrentItem() == null || MaterialUtil.isAir(event.getCurrentItem().getType())) user.getData().counter.inventoryAverageHeuristicsMisclicks.increment();
                // Shift - Double - Click shortcut will generate a lot of clicks.
            else if (user.getData().object.lastMaterialClicked != event.getCurrentItem().getType())
                user.getInventoryBatch().addDataPoint(InventoryBatch.InventoryClick.fromClickEvent(event));
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        final var batchProcessor = new AverageHeuristicBatchProcessor(this);
        return ModuleLoader.builder(this)
                           .batchProcessor(batchProcessor)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(160, 1).build();
    }
}

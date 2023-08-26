package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.batch.InventoryBatch;
import de.photon.anticheataddition.util.minecraft.tps.TPSProvider;
import de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil;
import de.photon.anticheataddition.util.violationlevels.ViolationAggregation;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import de.photon.anticheataddition.util.violationlevels.threshold.ThresholdManagement;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.stream.Collectors;

public class Inventory extends ViolationModule implements Listener
{
    public static final Inventory INSTANCE = new Inventory();
    public static final double MIN_TPS = 19.0D;
    public static final int TELEPORT_BYPASS_TIME = 900;
    public static final int WORLD_CHANGE_BYPASS_TIME = 2000;

    @Getter private final int maxPing = loadInt(".max_ping", 400);

    protected Inventory()
    {
        super("Inventory", InventoryAverageHeuristic.INSTANCE,
              InventoryHit.INSTANCE,
              InventoryMove.INSTANCE,
              InventoryMultiInteraction.INSTANCE,
              InventoryPerfectExit.INSTANCE,
              InventoryRotation.INSTANCE,
              InventorySprinting.INSTANCE,
              InventoryStatistical.INSTANCE);
    }

    public static boolean teleportOrWorldChangeBypassed(User user)
    {
        return user.hasTeleportedRecently(TELEPORT_BYPASS_TIME) ||
               user.hasChangedWorldsRecently(WORLD_CHANGE_BYPASS_TIME);
    }

    public static boolean hasMinTPS()
    {
        return TPSProvider.INSTANCE.atLeastTPS(MIN_TPS);
    }

    private static boolean supportedClickType(ClickType type)
    {
        return switch (type) {
            case DROP, RIGHT, LEFT, SHIFT_LEFT, SHIFT_RIGHT -> true;
            default -> false;
        };
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
            hasMinTPS()) {
            if (event.getCurrentItem() == null || MaterialUtil.INSTANCE.isAir(event.getCurrentItem().getType())) user.getData().counter.inventoryAverageHeuristicsMisclicks.increment();
                // Shift - Double - Click shortcut will generate a lot of clicks.
            else if (user.getData().object.lastMaterialClicked != event.getCurrentItem().getType())
                user.getInventoryBatch().addDataPoint(InventoryBatch.InventoryClick.fromClickEvent(event));
        }
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return new ViolationAggregation(this,
                                        ThresholdManagement.loadThresholds(this.getConfigString() + ".thresholds"),
                                        this.getChildren().stream().filter(ViolationModule.class::isInstance).map(ViolationModule.class::cast).map(ViolationModule::getManagement).collect(Collectors.toUnmodifiableSet()));
    }
}

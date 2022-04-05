package de.photon.anticheataddition.modules.checks.inventory;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataKey;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.mathematics.Polynomial;
import de.photon.anticheataddition.util.minecraft.tps.TPSProvider;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class InventoryPerfectExit extends ViolationModule implements Listener
{
    public static final InventoryPerfectExit INSTANCE = new InventoryPerfectExit();

    private static final Polynomial VL_CALCULATOR = new Polynomial(-0.2857, 40);

    private final double minTps = loadDouble(".min_tps", 18.5);

    private InventoryPerfectExit()
    {
        super("Inventory.parts.PerfectExit");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(final InventoryCloseEvent event)
    {
        val user = User.getUser(event.getPlayer().getUniqueId());
        if (User.isUserInvalid(user, this)) return;

        // Creative-clear might trigger this.
        if (user.inAdventureOrSurvivalMode() &&
            // Minimum TPS before the check is activated as of a huge amount of fps
            TPSProvider.INSTANCE.atLeastTPS(minTps) &&
            // Inventory is empty
            InventoryUtil.isInventoryEmpty(event.getInventory()))
        {
            val passedTime = user.getTimestampMap().at(TimeKey.LAST_INVENTORY_CLICK_ON_ITEM).passedTime();
            if (user.getDataMap().getCounter(DataKey.Count.INVENTORY_PERFECT_EXIT_FAILS).conditionallyIncDec(passedTime <= 70)) {
                this.getManagement().flag(Flag.of(user)
                                              .setAddedVl(VL_CALCULATOR.apply(passedTime).intValue())
                                              .setDebug("Inventory-Debug | Player: " + user.getPlayer().getName() + " exits inventories in a bot-like way (D: " + passedTime + ')'));
            }
        }
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(400, 1).build();
    }
}

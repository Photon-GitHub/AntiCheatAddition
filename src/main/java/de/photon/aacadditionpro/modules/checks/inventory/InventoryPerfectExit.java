package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.DataKey;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.inventory.InventoryUtil;
import de.photon.aacadditionpro.util.mathematics.Polynomial;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.minecraft.tps.TPSProvider;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class InventoryPerfectExit extends ViolationModule implements Listener
{
    private static final Polynomial VL_CALCULATOR = new Polynomial(-0.2857, 40);

    @LoadFromConfiguration(configPath = ".min_tps")
    private double minTps;

    public InventoryPerfectExit()
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
            TPSProvider.INSTANCE.getTPS() > minTps &&
            // Inventory is empty
            InventoryUtil.isInventoryEmpty(event.getInventory()))
        {
            val passedTime = user.getTimestampMap().at(TimestampKey.LAST_INVENTORY_CLICK_ON_ITEM).passedTime();
            if (user.getDataMap().getCounter(DataKey.CounterKey.INVENTORY_PERFECT_EXIT_FAILS).conditionallyIncDec(passedTime <= 70)) {
                this.getManagement().flag(Flag.of(user)
                                              .setAddedVl(VL_CALCULATOR.apply(passedTime).intValue())
                                              .setEventNotCancelledAction(() -> DebugSender.getInstance().sendDebug("Inventory-Debug | Player: " + user.getPlayer().getName() + " exits inventories in a bot-like way (D: " + passedTime + ')')));
            }
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
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(400, 1).build();
    }
}

package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.Getter;
import lombok.val;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class InventoryHit extends ViolationModule implements Listener
{
    @Getter
    private static final InventoryHit instance = new InventoryHit();

    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private int cancelVl;

    public InventoryHit()
    {
        super("Inventory.parts.Hit");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
    {
        if (event.getDamager() instanceof Player) {
            val user = User.getUser(event.getDamager().getUniqueId());
            if (User.isUserInvalid(user, this)) return;

            // Is in Inventory (Detection)
            if (user.hasOpenInventory() &&
                // Have the inventory opened for some time
                user.notRecentlyOpenedInventory(1000) &&
                // Is a hit-attack
                event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK)
            {
                this.getManagement().flag(Flag.of(user)
                                              .setAddedVl(10)
                                              .setCancelAction(cancelVl, () -> event.setCancelled(true))
                                              .setEventNotCancelledAction(() -> DebugSender.getInstance().sendDebug("Inventory-Debug | Player: " + user.getPlayer().getName() + " hit an entity while having an open inventory.")));
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
                                       .withDecay(80, 1).build();
    }
}

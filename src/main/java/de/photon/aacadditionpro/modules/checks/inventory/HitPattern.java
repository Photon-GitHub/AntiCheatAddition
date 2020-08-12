package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.messaging.VerboseSender;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

class HitPattern implements ListenerModule
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private int cancelVl;

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
    {
        if (event.getDamager() instanceof Player) {
            final User user = UserManager.getUser(event.getDamager().getUniqueId());

            // Not bypassed
            if (User.isUserInvalid(user, this.getModuleType())) {
                return;
            }

            // Is in Inventory (Detection)
            if (user.hasOpenInventory() &&
                // Have the inventory opened for some time
                user.notRecentlyOpenedInventory(1000) &&
                // Is a hit-attack
                event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK)
            {
                Inventory.getInstance().getViolationLevelManagement().flag(user.getPlayer(), 10, cancelVl, () -> event.setCancelled(true),
                                                                           () -> VerboseSender.getInstance().sendVerboseMessage("Inventory-Verbose | Player: " + user.getPlayer().getName() + " hit an entity while having an open inventory."));
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
        return this.getModuleType().getConfigString() + ".parts.Hit";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.INVENTORY;
    }
}

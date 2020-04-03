package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import lombok.Getter;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

class HitPattern extends PatternModule.Pattern<User, EntityDamageByEntityEvent>
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private int cancelVl;

    @Override
    protected int process(User user, EntityDamageByEntityEvent event)
    {
        // Is in Inventory (Detection)
        if (user.hasOpenInventory() &&
            // Have the inventory opened for some time
            user.notRecentlyOpenedInventory(1000) &&
            // Is a hit-attack
            event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK)
        {
            message = "Inventory-Verbose | Player: " + user.getPlayer().getName() + " hit an entity while having an open inventory.";
            return 10;
        }
        return 0;
    }

    @Override
    public void cancelAction(User user, EntityDamageByEntityEvent event)
    {
        event.setCancelled(true);
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

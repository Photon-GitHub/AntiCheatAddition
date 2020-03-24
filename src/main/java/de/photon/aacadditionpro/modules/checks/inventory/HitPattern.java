package de.photon.aacadditionpro.modules.checks.inventory;

import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.olduser.UserOld;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import lombok.Getter;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

class HitPattern extends PatternModule.Pattern<UserOld, EntityDamageByEntityEvent>
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private int cancelVl;

    @Override
    protected int process(UserOld user, EntityDamageByEntityEvent event)
    {
        // Is in Inventory (Detection)
        if (user.getInventoryData().hasOpenInventory() &&
            // Have the inventory opened for some time
            user.getInventoryData().notRecentlyOpened(1000) &&
            // Is a hit-attack
            event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK)
        {
            message = "Inventory-Verbose | Player: " + user.getPlayer().getName() + " hit an entity while having an open inventory.";
            return 10;
        }
        return 0;
    }

    @Override
    public void cancelAction(UserOld user, EntityDamageByEntityEvent event)
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

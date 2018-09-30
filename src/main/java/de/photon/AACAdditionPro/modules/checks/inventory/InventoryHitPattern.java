package de.photon.AACAdditionPro.modules.checks.inventory;

import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import lombok.Getter;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class InventoryHitPattern extends PatternModule.Pattern<User, EntityDamageByEntityEvent>
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private static int cancelVL;

    @Override
    protected int process(User user, EntityDamageByEntityEvent event)
    {
        // Is in Inventory (Detection)
        if (user.getInventoryData().hasOpenInventory() &&
            // Have the inventory opened for some time
            user.getInventoryData().notRecentlyOpened(1000) &&
            // Is a hit-attack
            event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK)
        {
            return 10;
        }
        return 0;
    }

    @Override
    public String getConfigString()
    {
        return this.getModuleType().getConfigString() + ".parts.InventoryHit";
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.INVENTORY;
    }
}

package de.photon.AACAdditionPro.modules.checks.inventory;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.modules.ListenerModule;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.modules.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.Set;

public class Inventory implements ListenerModule, PatternModule, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 80L);

    private final Pattern<User, EntityDamageByEntityEvent> inventoryHitPattern = new InventoryHitPattern();

    // ------------------------------------------- BlockPlace Handling ---------------------------------------------- //

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void on(final EntityDamageByEntityEvent event)
    {
        if (event.getDamager() instanceof Player) {
            final User user = UserManager.getUser(event.getDamager().getUniqueId());

            // Not bypassed
            if (User.isUserInvalid(user, this.getModuleType())) {
                return;
            }

            vlManager.flag(user.getPlayer(), inventoryHitPattern.apply(user, event), InventoryHitPattern.getCancelVL(), () -> event.setCancelled(true), () -> {});
        }
    }

    @Override
    public Set<Pattern> getPatterns()
    {
        return ImmutableSet.of(inventoryHitPattern);
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }
}
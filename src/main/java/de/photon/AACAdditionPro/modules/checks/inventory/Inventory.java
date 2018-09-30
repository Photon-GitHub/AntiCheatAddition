package de.photon.AACAdditionPro.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.modules.ListenerModule;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PacketListenerModule;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.modules.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Set;

public class Inventory extends PacketAdapter implements ListenerModule, PacketListenerModule, PatternModule, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 80L);

    private final Pattern<User, EntityDamageByEntityEvent> hitPattern = new HitPattern();
    private final Pattern<User, InventoryClickEvent> sprintingPattern = new SprintingPattern();

    public Inventory()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOWEST,
              // Move
              PacketType.Play.Server.POSITION,
              PacketType.Play.Client.POSITION_LOOK);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onEntityDamageByEntity(final EntityDamageByEntityEvent event)
    {
        if (event.getDamager() instanceof Player) {
            final User user = UserManager.getUser(event.getDamager().getUniqueId());

            // Not bypassed
            if (User.isUserInvalid(user, this.getModuleType())) {
                return;
            }

            vlManager.flag(user.getPlayer(), hitPattern.apply(user, event), HitPattern.getCancelVL(), () -> event.setCancelled(true), () -> {});
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(final InventoryClickEvent event)
    {
        final User user = UserManager.getUser(event.getWhoClicked().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        vlManager.flag(user.getPlayer(), sprintingPattern.apply(user, event), SprintingPattern.getCancelVL(), () ->
        {
            event.setCancelled(true);
            InventoryUtils.syncUpdateInventory(user.getPlayer());
        }, () -> {});
    }

    @Override
    public Set<Pattern> getPatterns()
    {
        return ImmutableSet.of(hitPattern,
                               sprintingPattern);
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
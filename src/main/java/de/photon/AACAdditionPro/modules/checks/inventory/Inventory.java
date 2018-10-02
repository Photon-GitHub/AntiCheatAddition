package de.photon.AACAdditionPro.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.modules.ListenerModule;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PacketListenerModule;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.modules.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
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
    private final Pattern<User, PacketEvent> movePattern = new MovePattern();
    private final PacketPattern rotationPattern = new RotationPattern();
    private final Pattern<User, InventoryClickEvent> sprintingPattern = new SprintingPattern();

    public Inventory()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOWEST,
              // Move
              PacketType.Play.Server.POSITION,
              PacketType.Play.Client.POSITION_LOOK);
    }

    @Override
    public void onPacketReceiving(PacketEvent event)
    {
        if (event.isPlayerTemporary()) {
            return;
        }

        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }


        vlManager.flag(user.getPlayer(), movePattern.apply(user, event), HitPattern.getCancelVl(), () -> movePattern.cancelAction(user, event), () -> {});
        vlManager.flag(user.getPlayer(), rotationPattern.apply(user, event.getPacket()), HitPattern.getCancelVl(), () -> rotationPattern.cancelAction(user, event.getPacket()), () -> {});
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

            vlManager.flag(user.getPlayer(), hitPattern.apply(user, event), HitPattern.getCancelVl(), () -> hitPattern.cancelAction(user, event), () -> {});
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

        vlManager.flag(user.getPlayer(), sprintingPattern.apply(user, event), SprintingPattern.getCancelVL(), () -> sprintingPattern.cancelAction(user, event), () -> {});
    }

    @Override
    public Set<Pattern> getPatterns()
    {
        return ImmutableSet.of(hitPattern,
                               movePattern,
                               rotationPattern,
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
package de.photon.aacadditionpro.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PacketListenerModule;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Set;

public class Inventory extends PacketAdapter implements ListenerModule, PacketListenerModule, PatternModule, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 80L);

    private final HitPattern hitPattern = new HitPattern();
    private final MovePattern movePattern = new MovePattern();
    private final MultiInteractionPattern multiInteractionPattern = new MultiInteractionPattern();
    private final RotationPattern rotationPattern = new RotationPattern();
    private final SprintingPattern sprintingPattern = new SprintingPattern();

    public Inventory()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOWEST,
              // Look
              PacketType.Play.Client.LOOK,
              // Move
              PacketType.Play.Client.POSITION,
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


        vlManager.flag(user.getPlayer(), true, movePattern.apply(user, event), movePattern.getCancelVl(), () -> movePattern.cancelAction(user, event), () -> {});
        vlManager.flag(user.getPlayer(), true, rotationPattern.apply(user, event), -1, () -> {}, () -> {});
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

            vlManager.flag(user.getPlayer(), false, hitPattern.apply(user, event), hitPattern.getCancelVl(), () -> hitPattern.cancelAction(user, event), () -> {});
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void on(final InventoryClickEvent event)
    {
        final User user = UserManager.getUser(event.getWhoClicked().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        vlManager.flag(user.getPlayer(), false, sprintingPattern.apply(user, event), sprintingPattern.getCancelVl(), () -> sprintingPattern.cancelAction(user, event), () -> {});
        vlManager.flag(user.getPlayer(), false, multiInteractionPattern.apply(user, event), multiInteractionPattern.getCancelVl(), () -> multiInteractionPattern.cancelAction(user, event), () -> {});
    }

    @Override
    public Set<Pattern> getPatterns()
    {
        return ImmutableSet.of(hitPattern,
                               movePattern,
                               multiInteractionPattern,
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
        return ModuleType.INVENTORY;
    }
}
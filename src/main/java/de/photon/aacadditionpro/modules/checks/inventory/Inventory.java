package de.photon.aacadditionpro.modules.checks.inventory;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.PacketListenerModule;
import de.photon.aacadditionpro.modules.PatternModule;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.modules.checks.autofish.ConsistencyPattern;
import de.photon.aacadditionpro.modules.checks.autofish.InhumanReactionPattern;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Set;

public class Inventory extends PacketAdapter implements ListenerModule, PacketListenerModule, PatternModule, ViolationModule
{
    @Getter
    private static final Inventory instance = new Inventory();
    private static final Set<Module> submodules = ImmutableSet.of(ConsistencyPattern.getInstance(), InhumanReactionPattern.getInstance());

    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 80L);

    private final AverageHeuristicPattern averageHeuristicPattern = new AverageHeuristicPattern();
    private final HitPattern hitPattern = new HitPattern();
    private final MovePattern movePattern = new MovePattern();
    private final MultiInteractionPattern multiInteractionPattern = new MultiInteractionPattern();
    private final PerfectExitPattern perfectExitPattern = new PerfectExitPattern();
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
        final User user = PacketListenerModule.safeGetUserFromEvent(event);

        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        vlManager.flag(user.getPlayer(), movePattern.apply(user, event), movePattern.getCancelVl(), () -> movePattern.cancelAction(user, event), () -> {});
        vlManager.flag(user.getPlayer(), rotationPattern.apply(user, event), -1, () -> {}, () -> {});
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

            vlManager.flag(user.getPlayer(), hitPattern.apply(user, event), hitPattern.getCancelVl(), () -> hitPattern.cancelAction(user, event), () -> {});
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClick(final InventoryClickEvent event)
    {
        final User user = UserManager.getUser(event.getWhoClicked().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        vlManager.flag(user.getPlayer(), sprintingPattern.apply(user, event), sprintingPattern.getCancelVl(), () -> sprintingPattern.cancelAction(user, event), () -> {});
        vlManager.flag(user.getPlayer(), multiInteractionPattern.apply(user, event), multiInteractionPattern.getCancelVl(), () -> multiInteractionPattern.cancelAction(user, event), () -> {});
        vlManager.flag(user.getPlayer(), averageHeuristicPattern.apply(user, event), 0, () -> {}, () -> {});
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInventoryClose(final InventoryCloseEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        vlManager.flag(user.getPlayer(), perfectExitPattern.apply(user, event), 0, () -> {}, () -> {});
    }

    @Override
    public Set<Module> getSubModules()
    {
        return ImmutableSet.of(averageHeuristicPattern,
                               hitPattern,
                               movePattern,
                               multiInteractionPattern,
                               perfectExitPattern,
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
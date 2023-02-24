package de.photon.anticheataddition.modules.checks.shield;

import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class ShieldHit extends ViolationModule implements Listener
{
    public static final ShieldHit INSTANCE = new ShieldHit();

    private final int cancelVl = loadInt(".cancel_vl", 60);

    private ShieldHit()
    {
        super("Shield.parts.Hit");
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event)
    {
        if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK &&
            event.getDamager() instanceof Player player)
        {
            final var user = User.getUser(player.getUniqueId());
            if (User.isUserInvalid(user, this)) return;

            // Player is blocking and hitting at the same time.
            if (player.isBlocking()) {
                this.getManagement().flag(Flag.of(user)
                                              .setAddedVl(20)
                                              .setCancelAction(cancelVl, () -> event.setCancelled(true))
                                              .setDebug(() -> "Shield-Debug | Player: " + user.getPlayer().getName() + " hit an entity while blocking with a shield."));
            }
        }
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .emptyThresholdManagement()
                                       .withDecay(160, 2).build();
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.MC115.getSupVersionsFrom())
                           .build();
    }
}

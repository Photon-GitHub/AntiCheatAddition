package de.photon.aacadditionpro.modules.checks.autoeat;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class AutoEat extends ViolationModule implements Listener
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancelVl;
    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    public AutoEat()
    {
        super("AutoEat");
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event)
    {
        val user = User.getUser(event.getPlayer().getUniqueId());
        if (User.isUserInvalid(user, bypassPermission)) return;

        Bukkit.getScheduler().runTaskLater(AACAdditionPro.getInstance(), () -> {
            // A PlayerInteractEvent will always fire when the right mouse button is clicked, therefore a legit player will always hold his mouse a bit longer than a bot and the last right click will
            // be after the last consume event.
            if (user.getTimestampMap().at(TimestampKey.LAST_RIGHT_CLICK_CONSUMABLE_ITEM_EVENT).getTime() < user.getTimestampMap().at(TimestampKey.LAST_CONSUME_EVENT).getTime()) {
                this.getManagement().flag(Flag.of(user)
                                              .setAddedVl(20)
                                              .setCancelAction(cancelVl, () -> user.getTimestampMap().at(TimestampKey.AUTOEAT_TIMEOUT).update()));
            }
        }, 10L);

        // Timeout
        if (user.getTimestampMap().at(TimestampKey.AUTOEAT_TIMEOUT).recentlyUpdated(timeout)) event.setCancelled(true);
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).withDecay(6000L, 20).build();
    }
}

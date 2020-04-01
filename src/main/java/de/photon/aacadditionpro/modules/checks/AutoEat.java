package de.photon.aacadditionpro.modules.checks;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class AutoEat implements ListenerModule, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 6000L);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancelVl;

    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // After half a second check if it was the last interaction ticks check if this
        Bukkit.getScheduler().runTaskLater(AACAdditionPro.getInstance(), () -> {
            if (user.getAutoEatData().getTimeStamp(0) < user.getConsumeData().getTimeStamp(0)) {
                vlManager.flag(user.getPlayer(), cancelVl, () -> user.getAutoEatData().updateTimeStamp(1), () -> {});
            }
        }, 10);

        if (user.getTimestampMap().recentlyUpdated(TimestampKey.AUTOEAT_TIMEOUT, timeout)) {
            event.setCancelled(true);
        }
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.AUTO_EAT;
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }
}

package de.photon.aacadditionproold.modules.checks;

import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.modules.ListenerModule;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.ViolationModule;
import de.photon.aacadditionproold.user.TimestampKey;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.UserManager;
import de.photon.aacadditionproold.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionproold.util.violationlevels.ViolationLevelManagement;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class AutoEat implements ListenerModule, ViolationModule
{
    @Getter
    private static final AutoEat instance = new AutoEat();

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
            if (user.getTimestampMap().getTimeStamp(TimestampKey.LAST_RIGHT_CLICK_CONSUMABLE_ITEM_EVENT) < user.getTimestampMap().getTimeStamp(TimestampKey.LAST_CONSUME_EVENT)) {
                vlManager.flag(user.getPlayer(), cancelVl, () -> user.getTimestampMap().updateTimeStamp(TimestampKey.AUTOEAT_TIMEOUT), () -> {});
            }
        }, 10);

        if (user.getTimestampMap().recentlyUpdated(TimestampKey.AUTOEAT_TIMEOUT, timeout)) {
            event.setCancelled(true);
        }
    }

    @Override
    public boolean isSubModule()
    {
        return false;
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

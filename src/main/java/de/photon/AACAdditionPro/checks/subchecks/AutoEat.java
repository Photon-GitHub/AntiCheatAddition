package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public class AutoEat implements Listener, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 3500L);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;

    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    // Do not ignore cancelled ones as the check is supposed to catch e.g. fastuse users as well.
    @EventHandler
    public void on(final PlayerItemConsumeEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // User valid and not bypassed
        if (User.isUserInvalid(user))
        {
            return;
        }

        // Timeout
        if (user.getAutoEatData().recentlyUpdated(1, this.timeout))
        {
            event.setCancelled(true);
            return;
        }

        // Check for bot like interaction
        // TODO: ACTUAL VALUE INSTEAD OF 500
        if (user.getAutoEatData().recentlyUpdated(0, 500))
        {
            this.vlManager.flag(event.getPlayer(), this.cancel_vl, () -> {
                event.setCancelled(true);
                // Timeout
                user.getAutoEatData().updateTimeStamp(1);
            }, () -> {});
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

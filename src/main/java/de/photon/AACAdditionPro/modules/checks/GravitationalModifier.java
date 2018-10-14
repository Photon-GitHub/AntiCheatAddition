package de.photon.AACAdditionPro.modules.checks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.modules.ListenerModule;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.Bukkit;

public class GravitationalModifier implements ListenerModule, ViolationModule
{
    private static final int MAX_VELOCITY_CHANGES = 12;

    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 120);

    @Override
    public void enable()
    {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), () -> {
            for (User user : UserManager.getUsersUnwrapped()) {
                // Not bypassed
                if (User.isUserInvalid(user, this.getModuleType())) {
                    continue;
                }

                // Time of a check cycle is over
                if (!user.getVelocityChangeData().recentlyUpdated(0, 3000)) {
                    this.vlManager.flag(user.getPlayer(),
                                        user.getVelocityChangeData().velocityChangeCounter - MAX_VELOCITY_CHANGES,
                                        1,
                                        () -> {},
                                        () -> {});

                    user.getVelocityChangeData().velocityChangeCounter = 0;
                    user.getVelocityChangeData().updateTimeStamp(0);
                }
            }
        }, 20, 60);
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.GRAVITATIONAL_MODIFIER;
    }
}

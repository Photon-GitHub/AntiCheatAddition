package de.photon.aacadditionpro.modules.checks;

import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.olduser.UserManager;
import de.photon.aacadditionpro.olduser.UserOld;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ImpossibleChat implements ListenerModule, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 600);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancelVl;

    @EventHandler
    public void on(final AsyncPlayerChatEvent event)
    {
        final UserOld user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (UserOld.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // Is in Inventory (Detection)
        if (user.getPlayer().isSprinting() ||
            user.getPlayer().isSneaking() ||
            user.getPlayer().isBlocking() ||
            user.getPlayer().isDead() ||
            (user.getInventoryData().hasOpenInventory() &&
             // Have the inventory opened for some time
             user.getInventoryData().notRecentlyOpened(1000)
            ))
        {
            vlManager.flag(user.getPlayer(), cancelVl, () -> event.setCancelled(true), () -> {});
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.IMPOSSIBLE_CHAT;
    }
}

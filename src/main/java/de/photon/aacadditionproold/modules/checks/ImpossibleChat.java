package de.photon.aacadditionproold.modules.checks;

import de.photon.aacadditionproold.modules.ListenerModule;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.modules.ViolationModule;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.UserManager;
import de.photon.aacadditionproold.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionproold.util.violationlevels.ViolationLevelManagement;
import lombok.Getter;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ImpossibleChat implements ListenerModule, ViolationModule
{
    @Getter
    private static final ImpossibleChat instance = new ImpossibleChat();

    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 600);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancelVl;

    @EventHandler
    public void onAsyncChat(final AsyncPlayerChatEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // Is in Inventory (Detection)
        if (user.getPlayer().isSprinting() ||
            user.getPlayer().isSneaking() ||
            user.getPlayer().isBlocking() ||
            user.getPlayer().isDead() ||
            (user.hasOpenInventory() &&
             // Have the inventory opened for some time
             user.notRecentlyOpenedInventory(1000)
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
    public boolean isSubModule()
    {
        return false;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.IMPOSSIBLE_CHAT;
    }
}

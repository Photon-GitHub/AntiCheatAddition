package de.photon.aacadditionpro.modules.checks.impossiblechat;

import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ImpossibleChat extends ViolationModule implements Listener
{
    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancelVl;

    public ImpossibleChat()
    {
        super("ImpossibleChat");
    }

    @EventHandler
    public void onAsyncChat(final AsyncPlayerChatEvent event)
    {
        val user = User.getUser(event.getPlayer().getUniqueId());
        if (User.isUserInvalid(user, this)) return;

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
            this.getManagement().flag(Flag.of(user).setCancelAction(cancelVl, () -> event.setCancelled(true)));
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).withDecay(600, 1).build();
    }
}

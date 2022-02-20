package de.photon.aacadditionpro.modules.checks.impossiblechat;

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
        val user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        if (user.getPlayer().isSprinting() ||
            user.getPlayer().isSneaking() ||
            user.getPlayer().isBlocking() ||
            user.getPlayer().isDead() ||
            // Have the inventory opened for some time
            (user.hasOpenInventory() && user.notRecentlyOpenedInventory(1000)))
        {
            this.getManagement().flag(Flag.of(user).setAddedVl(25).setCancelAction(cancelVl, () -> event.setCancelled(true)));
        }
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).loadThresholdsToManagement().withDecay(600, 25).build();
    }
}

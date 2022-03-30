package de.photon.anticheataddition.modules.checks.impossiblechat;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public final class ImpossibleChat extends ViolationModule implements Listener
{
    public static final ImpossibleChat INSTANCE = new ImpossibleChat();

    private final int cancelVl = loadInt(".cancel_vl", 75);

    private ImpossibleChat()
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

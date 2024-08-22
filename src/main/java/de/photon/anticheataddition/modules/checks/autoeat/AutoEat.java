package de.photon.anticheataddition.modules.checks.autoeat;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemConsumeEvent;

public final class AutoEat extends ViolationModule implements Listener
{
    public static final AutoEat INSTANCE = new AutoEat();

    private final int cancelVl = loadInt(".cancel_vl", 80);
    private final int timeout = loadInt(".timeout", 5000);

    private AutoEat()
    {
        super("AutoEat");
    }

    @EventHandler(ignoreCancelled = true)
    public void onConsume(PlayerItemConsumeEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        // If the amount is 1, the last right click on a consumable will be perfect (bot-like), as the item disappears from the slot.
        if (User.isUserInvalid(user, this) || event.getItem().getAmount() <= 1) return;

        Bukkit.getScheduler().runTaskLater(AntiCheatAddition.getInstance(), () -> {
            // A PlayerInteractEvent will always fire when the right mouse button is clicked, therefore a legit player will always hold his mouse a bit longer than a bot and the last right click will
            // be after the last consume event.
            if (user.getTimeMap().at(TimeKey.RIGHT_CLICK_CONSUMABLE_ITEM_EVENT).getTime() < user.getTimeMap().at(TimeKey.CONSUME_EVENT).getTime()) {
                this.getManagement().flag(Flag.of(user)
                                              .setAddedVl(20)
                                              .setCancelAction(cancelVl, () -> user.getTimeMap().at(TimeKey.AUTOEAT_TIMEOUT).update()));
            }
        }, 10L);

        // Timeout
        if (user.getTimeMap().at(TimeKey.AUTOEAT_TIMEOUT).recentlyUpdated(timeout)) event.setCancelled(true);
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).loadThresholdsToManagement().withDecay(6000L, 20).build();
    }
}

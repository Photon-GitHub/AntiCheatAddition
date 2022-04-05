package de.photon.anticheataddition.modules.checks.autoeat;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
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
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (user == null) return;

        if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR) &&
            event.getMaterial().isEdible() &&
            // A sudden stop in sprinting after a lost food level.
            user.hasSprintedRecently(500) &&
            // Too low to sprint, forced by minecraft.
            user.getPlayer().getFoodLevel() > 6 &&
            user.getTimestampMap().at(TimeKey.FOOD_LEVEL_LOST).recentlyUpdated(100))
            this.getManagement().flag(Flag.of(user)
                                          .setAddedVl(5)
                                          .setCancelAction(cancelVl, () -> user.getTimestampMap().at(TimeKey.AUTOEAT_TIMEOUT).update()));
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event)
    {
        val user = User.getUser(event.getPlayer());
        // If the amount is 1, the last right click on a consumable will be perfect (bot-like), as the item disappears from the slot.
        if (User.isUserInvalid(user, this) || event.getItem().getAmount() <= 1) return;

        Bukkit.getScheduler().runTaskLater(AntiCheatAddition.getInstance(), () -> {
            // A PlayerInteractEvent will always fire when the right mouse button is clicked, therefore a legit player will always hold his mouse a bit longer than a bot and the last right click will
            // be after the last consume event.
            if (user.getTimestampMap().at(TimeKey.RIGHT_CLICK_CONSUMABLE_ITEM_EVENT).getTime() < user.getTimestampMap().at(TimeKey.CONSUME_EVENT).getTime()) {
                this.getManagement().flag(Flag.of(user)
                                              .setAddedVl(20)
                                              .setCancelAction(cancelVl, () -> user.getTimestampMap().at(TimeKey.AUTOEAT_TIMEOUT).update()));
            }
        }, 10L);

        // Timeout
        if (user.getTimestampMap().at(TimeKey.AUTOEAT_TIMEOUT).recentlyUpdated(timeout)) event.setCancelled(true);
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).loadThresholdsToManagement().withDecay(6000L, 20).build();
    }
}

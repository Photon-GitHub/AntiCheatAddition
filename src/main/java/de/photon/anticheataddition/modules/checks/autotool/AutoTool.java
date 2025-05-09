package de.photon.anticheataddition.modules.checks.autotool;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.minecraft.ping.PingProvider;
import de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * AutoTool – detects suspicious tool swaps,
 * including the “Switch Back” option.
 */
public final class AutoTool extends ViolationModule implements Listener
{
    public static final AutoTool INSTANCE = new AutoTool();

    private AutoTool() {super("AutoTool");}

    private final int cancelVl = loadInt(".cancel_vl", 0);
    private final int timeout = loadInt(".timeout", 1500);
    private final int maxPing = loadInt(".max_ping", 400);
    private final int backSwitchDelay = loadInt(".back_switch_delay", 150);
    private final int minSwitchDelay = loadInt(".min_switch_delay", 150);
    private final long streakWindow = loadLong(".streak_window", 5000);

    /* ───────── per-player scratch data ───────── */

    private record Swap(long time, int fromSlot, int toSlot, ItemStack fromItem, ItemStack toItem)
    {
        private static Swap fromEvent(long time, PlayerItemHeldEvent e)
        {
            return new Swap(time,
                            e.getPreviousSlot(), e.getNewSlot(),
                            e.getPlayer().getInventory().getItem(e.getPreviousSlot()),
                            e.getPlayer().getInventory().getItem(e.getNewSlot()));
        }
    }

    private record Click(long time, Location loc, Material block, int slot, ItemStack heldAtClick)
    {
        private static Click fromEvent(long time, PlayerInteractEvent e)
        {
            return new Click(time,
                             e.getClickedBlock().getLocation(),
                             e.getClickedBlock().getType(),
                             e.getPlayer().getInventory().getHeldItemSlot(),
                             e.getPlayer().getInventory().getItem(e.getPlayer().getInventory().getHeldItemSlot()));
        }
    }

    public record AutoToolData(Swap lastSwap, Click lastClick, long digStart, long lastCorrectSwapTime, int originalSlot)
    {
        private AutoToolData replaceLastSwap(Swap lastSwap)
        {
            return new AutoToolData(lastSwap, this.lastClick, this.digStart, this.lastCorrectSwapTime, this.originalSlot);
        }

        private AutoToolData finishedSwap()
        {
            return new AutoToolData(this.lastSwap, this.lastClick, this.digStart, 0, -1);
        }
    }

    /* ───────── events ───────── */

    @EventHandler(ignoreCancelled = true)
    public void onHotbarSwap(PlayerItemHeldEvent e)
    {
        final User user = User.getUser(e.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        // cancel-VL timeout (inactive when cancel_vl = 0)
        if (user.getTimeMap().at(TimeKey.AUTOTOOL_TIMEOUT).recentlyUpdated(timeout)) {
            e.setCancelled(true);
            InventoryUtil.syncUpdateInventory(e.getPlayer());
            return;
        }

        // Detects switching back to the original tool too quickly
        final long now = System.currentTimeMillis();
        AutoToolData data = user.getData().object.autoToolData;
        if (data.lastCorrectSwapTime > 0 &&
            now - data.lastCorrectSwapTime <= backSwitchDelay &&
            e.getNewSlot() == data.originalSlot) {
            this.getManagement().flag(Flag.of(user).setAddedVl(10).setCancelAction(cancelVl, () -> {
                e.setCancelled(true);
                InventoryUtil.syncUpdateInventory(e.getPlayer());
                user.getTimeMap().at(TimeKey.AUTOTOOL_TIMEOUT).update();
            }));
            data = data.finishedSwap();
        }

        user.getData().object.autoToolData = data.replaceLastSwap(Swap.fromEvent(now, e));
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeftClick(PlayerInteractEvent e)
    {
        if (e.getAction() != Action.LEFT_CLICK_BLOCK || e.getClickedBlock() == null) return;

        final User user = User.getUser(e.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        final Click c = Click.fromEvent(System.currentTimeMillis(), e);

        /* new dig session ALWAYS on mouse-down */
        user.getData().object.autoToolData = new AutoToolData(null, c, c.time(), 0, -1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapAfterClick(PlayerItemHeldEvent e)
    {
        final User user = User.getUser(e.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        final AutoToolData data = user.getData().object.autoToolData;
        if (data.lastClick == null) return;

        final long now = System.currentTimeMillis();
        final long delay = now - data.lastClick.time();
        if (delay > minSwitchDelay ||
            // late correction
            now - data.digStart >= minSwitchDelay) return;

        final ItemStack was = data.lastClick.heldAtClick();
        final ItemStack nowItem = e.getPlayer().getInventory().getItem(e.getNewSlot());

        evaluateSuspicion(user, data, was, nowItem, delay, data.lastClick.block(), data.lastClick.slot());
    }

    /* swap-before-hit */

    /* ───────── core logic ───────── */

    private void evaluateSuspicion(@NotNull User user, AutoToolData data,
                                   @Nullable ItemStack itemBeforeSwitch, @Nullable ItemStack itemAfterSwitch,
                                   long delay, Material block, int originalSlot)
    {
        // Ignore if the player is not holding a tool
        if (itemAfterSwitch == null) return;
        if (itemBeforeSwitch == null) itemBeforeSwitch = new ItemStack(Material.AIR);

        // Tool was already correct
        if (isCorrectTool(itemBeforeSwitch, block) ||
            // Swapped to wrong tool
            !isCorrectTool(itemAfterSwitch, block) ||
            // Too high player ping
            !PingProvider.INSTANCE.atMostMaxPing(user.getPlayer(), maxPing)) return;

        int vl = 10;
        if (delay <= 80) vl += 10;

        // Check if the player is in a streak.
        if (user.getTimeMap().at(TimeKey.AUTOTOOL_STREAK_START).recentlyUpdated(streakWindow)) {
            // If the player is in a streak increment the streak counter.
            if (user.getData().counter.autoToolStreak.incrementCompareThreshold()) {
                vl += 10;
            }
        } else {
            // If the player is not in a streak, start a streak now.
            user.getTimeMap().at(TimeKey.AUTOTOOL_STREAK_START).update();
            user.getData().counter.autoToolStreak.setToZero();
        }

        user.getData().object.autoToolData = new AutoToolData(data.lastSwap, data.lastClick, data.digStart, System.currentTimeMillis(), originalSlot);
        getManagement().flag(Flag.of(user)
                                 .setAddedVl(vl)
                                 .setCancelAction(cancelVl, () -> {
                                     InventoryUtil.syncUpdateInventory(user.getPlayer());
                                     user.getTimeMap().at(TimeKey.AUTOTOOL_TIMEOUT).update();
                                 }));
    }

    private static boolean isCorrectTool(ItemStack tool, Material minedMaterial)
    {
        if (tool == null) return false;
        final Material toolType = tool.getType();

        // Special case for shears as there are no tags for them.
        if (toolType == Material.SHEARS)
            return minedMaterial.name().contains("LEAVES") || minedMaterial.name().contains("WOOL") || minedMaterial.name().equals("COBWEB");

        return MaterialUtil.INSTANCE.correctToolType(minedMaterial, toolType);
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .loadThresholdsToManagement()
                                       .withDecay(2400L, 30)
                                       .build();
    }
}

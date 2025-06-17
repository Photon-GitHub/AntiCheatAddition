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
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * {@code AutoTool} detects suspicious quick tool swaps around block interactions,
 * aiming to identify and flag automated or illicit "auto-tool" behaviors.
 * <p>
 * This module watches for rapid hotbar switches before breaking blocks,
 * enforces timeouts, streak penalties, and has ping-based exemptions.
 * </p>
 */
public final class AutoTool extends ViolationModule implements Listener
{
    public static final AutoTool INSTANCE = new AutoTool();
    private static final ItemStack AIR_STACK = new ItemStack(Material.AIR);

    private AutoTool()
    {
        super("AutoTool");
    }

    private final int cancelVl = loadInt(".cancel_vl", 0);
    private final int timeout = loadInt(".timeout", 1500);
    private final int maxPing = loadInt(".max_ping", 400);
    private final int backSwitchDelay = loadInt(".back_switch_delay", 150);
    private final int minSwitchDelay = loadInt(".min_switch_delay", 150);
    private final long streakWindow = loadLong(".streak_window", 5000);

    /* ───────── Internal data structures ───────── */

    /**
     * Represents a left-click interaction on a block with context.
     */
    private record Click(Location loc, Material block, int slot, ItemStack heldAtClick)
    {
        private static Click fromEvent(PlayerInteractEvent e)
        {
            return new Click(e.getClickedBlock().getLocation(), e.getClickedBlock().getType(), e.getPlayer().getInventory().getHeldItemSlot(), e.getPlayer().getInventory().getItem(e.getPlayer().getInventory().getHeldItemSlot()));
        }
    }

    /**
     * Immutable data for tracking a player's auto-tool state between events.
     *
     * @param lastClick    the last recorded block click
     * @param originalSlot the original slot index before swap
     */
    public record AutoToolData(Click lastClick, int originalSlot)
    {
        private AutoToolData finishedSwap()
        {
            return new AutoToolData(this.lastClick, -1);
        }
    }

    /* ───────── Event handlers ───────── */

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
        AutoToolData data = user.getData().object.autoToolData;
        if (user.getTimeMap().at(TimeKey.AUTOTOOL_LAST_CORRECT_SWAP).recentlyUpdated(backSwitchDelay) &&
            e.getNewSlot() == data.originalSlot) {
            autoToolFlag(user, 10, e);
            user.getData().object.autoToolData = data.finishedSwap();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLeftClick(PlayerInteractEvent e)
    {
        if (e.getAction() != Action.LEFT_CLICK_BLOCK || e.getClickedBlock() == null) return;

        final User user = User.getUser(e.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        final Click c = Click.fromEvent(e);

        /* new dig session ALWAYS on mouse-down */
        user.getTimeMap().at(TimeKey.AUTOTOOL_DIG_START).update();
        user.getData().object.autoToolData = new AutoToolData(c, -1);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwapAfterClick(PlayerItemHeldEvent e)
    {
        final User user = User.getUser(e.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        final AutoToolData data = user.getData().object.autoToolData;
        if (data.lastClick == null) return;

        final long delay = user.getTimeMap().at(TimeKey.AUTOTOOL_DIG_START).passedTime();

        // Late correction
        if (delay > minSwitchDelay) return;

        final ItemStack was = data.lastClick.heldAtClick();
        final ItemStack nowItem = e.getPlayer().getInventory().getItem(e.getNewSlot());

        evaluateSuspicion(user, data, was, nowItem, delay, data.lastClick.block(), data.lastClick.slot());
    }

    /* swap-before-hit */

    /* ───────── core logic ───────── */

    /**
     * Analyzes a swap action's context to determine if it is suspicious.
     * Flags violations based on tool correctness, delay thresholds, ping, and streaks.
     *
     * @param user             the user being evaluated
     * @param data             prior auto-tool state
     * @param itemBeforeSwitch tool held at click time (may be AIR)
     * @param itemAfterSwitch  tool held after swap
     * @param delay            time between click and swap in milliseconds
     * @param block            block material being mined
     * @param originalSlot     slot index before the swap
     */
    private void evaluateSuspicion(@NotNull User user, AutoToolData data, @Nullable ItemStack itemBeforeSwitch, @Nullable ItemStack itemAfterSwitch, long delay, Material block, int originalSlot)
    {
        // Ignore if the player is not holding a tool
        if (itemAfterSwitch == null) return;
        if (itemBeforeSwitch == null) itemBeforeSwitch = AIR_STACK;

        // Tool was already correct
        if (isCorrectTool(itemBeforeSwitch, block) ||
            // Too high player ping
            !PingProvider.INSTANCE.atMostMaxPing(user.getPlayer(), maxPing)) return;

        // Swapped to wrong tool
        if (!isCorrectTool(itemAfterSwitch, block)) {
            user.getData().counter.autoToolCorrectSwitches.decrementAboveZero();
            return;
        }

        // Enough correct switches to flag.
        if (!user.getData().counter.autoToolCorrectSwitches.incrementCompareThreshold()) return;

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

        user.getTimeMap().at(TimeKey.AUTOTOOL_LAST_CORRECT_SWAP).update();
        user.getData().object.autoToolData = new AutoToolData(data.lastClick, originalSlot);
        this.autoToolFlag(user, vl, null);
    }

    /**
     * Determines whether the given tool is correct for mining the specified material.
     * Includes a special-case for shears.
     *
     * @param tool          the ItemStack representing the tool
     * @param minedMaterial the Material being mined
     *
     * @return {@code true} if the tool is appropriate, {@code false} otherwise
     */
    private static boolean isCorrectTool(ItemStack tool, Material minedMaterial)
    {
        if (tool == null || minedMaterial == null) return false;
        final Material toolType = tool.getType();
        // Early return for air
        if (toolType == Material.AIR) return false;

        // Special case for shears as there are no tags for them.
        if (toolType == Material.SHEARS)
            return minedMaterial.name().contains("LEAVES") || minedMaterial.name().contains("WOOL") || minedMaterial.name().equals("COBWEB");

        return MaterialUtil.INSTANCE.correctToolType(minedMaterial, toolType);
    }

    private void autoToolFlag(User user, int vl, @Nullable Cancellable eventToCancel)
    {
        this.getManagement().flag(Flag.of(user).setAddedVl(vl).setCancelAction(cancelVl, () -> {
            if (eventToCancel != null) eventToCancel.setCancelled(true);
            InventoryUtil.syncUpdateInventory(user.getPlayer());
            user.getTimeMap().at(TimeKey.AUTOTOOL_TIMEOUT).update();
        }));
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

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

    public record AutoToolData(Swap lastSwap, Click lastClick, long digStart, int streak, long streakStart, long lastCorrectSwapTime, int originalSlot)
    {
        private AutoToolData replaceLastSwap(Swap lastSwap)
        {
            return new AutoToolData(lastSwap, this.lastClick, this.digStart, this.streak, this.streakStart, this.lastCorrectSwapTime, this.originalSlot);
        }

        private AutoToolData finishedSwap()
        {
            return new AutoToolData(this.lastSwap, this.lastClick, this.digStart, this.streak, this.streakStart, 0, -1);
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
        user.getData().object.autoToolData = new AutoToolData(null, c, c.time(), 0, 0, 0, -1);
        evaluateBeforeHit(user, e.getPlayer(), c);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapAfterClick(PlayerItemHeldEvent e)
    {
        User user = User.getUser(e.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        AutoToolData data = user.getData().object.autoToolData;
        if (data == null || data.lastClick == null) return;

        long now = System.currentTimeMillis();
        long delay = now - data.lastClick.time();
        if (delay > minSwitchDelay) return;
        if (now - data.digStart >= minSwitchDelay) return;           // late correction

        ItemStack was = data.lastClick.heldAtClick();
        ItemStack nowItem = e.getPlayer().getInventory().getItem(e.getNewSlot());

        evaluateSuspicion(user, e.getPlayer(), data, was, nowItem, delay, data.lastClick.block(), data.lastClick.slot());
    }

    /* swap-before-hit */
    private void evaluateBeforeHit(User user, org.bukkit.entity.Player p, Click c)
    {
        AutoToolData data = user.getData().object.autoToolData;
        if (data == null || data.lastSwap == null) return;

        long delay = c.time() - data.lastSwap.time();
        if (delay < 0 || delay > minSwitchDelay) return;
        if (data.lastSwap.time() - data.digStart >= minSwitchDelay) return;  // late correction

        evaluateSuspicion(user, p, data,
                          data.lastSwap.fromItem(), data.lastSwap.toItem(),
                          delay, c.block(), data.lastSwap.fromSlot());
    }

    /* ───────── core logic ───────── */

    private void evaluateSuspicion(User user, org.bukkit.entity.Player p, AutoToolData d,
                                   ItemStack wrongRaw, ItemStack right,
                                   long delay, Material block, int originalSlot)
    {

        if (right == null) return;                       // empty hand
        ItemStack wrong = wrongRaw == null ? new ItemStack(Material.AIR) : wrongRaw;

        if (!isCorrectTool(right, block)) return;        // swapped to wrong tool
        if (wrong.getType() != Material.AIR && isCorrectTool(wrong, block)) return; // both good
        if (!PingProvider.INSTANCE.atMostMaxPing(p, loadInt(".max_ping", 400))) return;

        int add = (delay <= 80) ? 20 : 10;

        long now = System.currentTimeMillis();
        int st = (now - d.streakStart <= streakWindow) ? d.streak + 1 : 1;
        long ss = (st == 1) ? now : d.streakStart;
        if (st >= 4) add += 30;

        user.getData().object.autoToolData = new AutoToolData(d.lastSwap, d.lastClick, d.digStart, st, ss, now, originalSlot);
        getManagement().flag(Flag.of(user)
                                 .setAddedVl(add)
                                 .setCancelAction(cancelVl, () -> {
                                     InventoryUtil.syncUpdateInventory(p);
                                     user.getTimeMap().at(TimeKey.AUTOTOOL_TIMEOUT).update();
                                 }));
    }

    private static boolean isCorrectTool(ItemStack tool, Material minedMaterial)
    {
        if (tool == null) return false;
        final Material toolType = tool.getType();

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

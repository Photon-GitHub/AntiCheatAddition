package de.photon.anticheataddition.modules.checks.autotool;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.minecraft.ping.PingProvider;
import de.photon.anticheataddition.util.minecraft.tps.TPSProvider;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AutoTool detection – flags modules that switch to the best tool instantly.
 * <p>
 * The added VL scales with the delay: &lt;50 ms → 30 VL, &lt;=100 ms → 20 VL,
 * &lt;=min_switch_delay ms → 10 VL, otherwise 5 VL.
 */
public final class AutoTool extends ViolationModule implements Listener {
    public static final AutoTool INSTANCE = new AutoTool();

    // stores last left‑click on a block
    private static final Map<UUID, Long> LAST_CLICK = new ConcurrentHashMap<>();

    /* ─────── Configurable values ─────── */
    private final int  cancelVl        = loadInt(".cancel_vl",        60);
    private final int  maxPing         = loadInt(".max_ping",         400);
    private final int  minSwitchDelay  = loadInt(".min_switch_delay", 150);
    private final int  timeout         = loadInt(".timeout",          3000);

    private AutoTool() { super("AutoTool"); }

    /* ────────────────────── Events ────────────────────── */

    @EventHandler(ignoreCancelled = true)
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        LAST_CLICK.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHotbarSwitch(PlayerItemHeldEvent event) {
        var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        // Global exemptions
        if (!TPSProvider.INSTANCE.atLeastTPS(19) ||
            !PingProvider.INSTANCE.atMostMaxPing(user.getPlayer(), maxPing) ||
            canBeLegit(event.getPreviousSlot(), event.getNewSlot())) return;

        // Timeout from previous heavy violation
        if (user.getTimeMap().at(TimeKey.AUTOTOOL_TIMEOUT).recentlyUpdated(timeout)) {
            event.setCancelled(true);
            return;
        }

        long click = LAST_CLICK.getOrDefault(event.getPlayer().getUniqueId(), -1L);
        long delay = System.currentTimeMillis() - click;
        if (click == -1L || delay > minSwitchDelay) return;

        ItemStack prev = event.getPlayer().getInventory().getItem(event.getPreviousSlot());
        ItemStack curr = event.getPlayer().getInventory().getItem(event.getNewSlot());

        if (!isTool(curr) || isTool(prev)) return;   // non‑tool → tool only

        int addedVl = vlFromDelay(delay);

        getManagement().flag(
            Flag.of(user)
                .setAddedVl(addedVl)
                .setCancelAction(cancelVl, () -> {
                    event.setCancelled(true);
                    InventoryUtil.syncUpdateInventory(user.getPlayer());
                    user.getTimeMap().at(TimeKey.AUTOTOOL_TIMEOUT).update();
                })
        );
    }

    /* ────────────────────── Helpers ────────────────────── */

    private int vlFromDelay(long delay) {
        if (delay <= 50) return 30;
        if (delay <= 100) return 20;
        if (delay <= minSwitchDelay) return 10;
        return 5;
    }

    private static boolean canBeLegit(int oldSlot, int newSlot) {
        return (oldSlot == 0 && newSlot == 8) ||
               (oldSlot == 8 && newSlot == 0) ||
               MathUtil.absDiff(oldSlot, newSlot) <= 1;
    }

    private static boolean isTool(ItemStack stack) { return stack != null && isTool(stack.getType()); }
    private static boolean isTool(Material m) {
        if (m == null) return false;
        String n = m.name();
        return n.endsWith("_PICKAXE") || n.endsWith("_AXE") ||
               n.endsWith("_SHOVEL")  || n.endsWith("_HOE") ||
               m == Material.SHEARS;
    }

    @Override
    protected ViolationManagement createViolationManagement() {
        return ViolationLevelManagement.builder(this).loadThresholdsToManagement().withDecay(6000L, 20).build();
    }
}

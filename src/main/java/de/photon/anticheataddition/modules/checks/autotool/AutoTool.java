package de.photon.anticheataddition.modules.checks.autotool;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
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
 * Flags the classic “AutoTool” hack – instant switch from a non-tool
 * to the perfect tool in ≤150 ms after the player left-clicks a block.
 */
public final class AutoTool extends ViolationModule implements Listener {
    public static final AutoTool INSTANCE = new AutoTool();

    private static final long  SWITCH_WINDOW = 150L;   // ms
    private static final int   MIN_NEIGHBOUR_DELAY = 50; // neighbour-slot scroll spam filter
    private static final Map<UUID, Long> LAST_CLICK = new ConcurrentHashMap<>();

    private final int cancelVl = loadInt(".cancel_vl", 50);
    private final int maxPing  = loadInt(".max_ping", 400);

    private AutoTool() { super("Autotool"); }

    /* ────────────────────── Events ────────────────────── */

    @EventHandler(ignoreCancelled = true)
    public void onLeftClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_BLOCK) return;
        LAST_CLICK.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @EventHandler(ignoreCancelled = true)
    public void onHotbarSwitch(PlayerItemHeldEvent event) {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this) ||
            !TPSProvider.INSTANCE.atLeastTPS(19) ||
            canBeLegit(event.getPreviousSlot(), event.getNewSlot())) return;

        long last = LAST_CLICK.getOrDefault(event.getPlayer().getUniqueId(), -1L);
        if (last == -1L || System.currentTimeMillis() - last > SWITCH_WINDOW) return;
        if (!PingProvider.INSTANCE.atMostMaxPing(user.getPlayer(), maxPing)) return;

        ItemStack prev = event.getPlayer().getInventory().getItem(event.getPreviousSlot());
        ItemStack curr = event.getPlayer().getInventory().getItem(event.getNewSlot());

        if (!isTool(curr) || isTool(prev)) return;   // only care about non-tool → tool

        getManagement().flag(
            Flag.of(user)
                .setAddedVl(20)
                .setCancelAction(cancelVl, () -> {
                    event.setCancelled(true);
                    InventoryUtil.syncUpdateInventory(user.getPlayer());
                })
        );
    }

    /* ────────────────────── Helpers ────────────────────── */

    private static boolean canBeLegit(int oldSlot, int newSlot) {
        return (oldSlot == 0 && newSlot == 8) ||
               (oldSlot == 8 && newSlot == 0) ||
               MathUtil.absDiff(oldSlot, newSlot) <= 1;
    }

    private static boolean isTool(ItemStack stack) { return stack != null && isTool(stack.getType()); }
    private static boolean isTool(Material m) {
        if (m == null) return false;
        String n = m.name();
        return n.endsWith("_PICKAXE") || n.endsWith("_AXE") || n.endsWith("_SHOVEL") ||
               n.endsWith("_HOE")     || m == Material.SHEARS;
    }

    @Override
    protected ViolationManagement createViolationManagement() {
        // same decay curve as Fastswitch – works fine
        return ViolationLevelManagement.builder(this)
                                       .loadThresholdsToManagement()
                                       .withDecay(120, 25)
                                       .build();
    }
}

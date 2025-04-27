package de.photon.anticheataddition.modules.checks.autotool;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.minecraft.ping.PingProvider;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.Data;
import org.bukkit.Material;
import org.bukkit.block.Block;
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
 * AutoTool check – v3
 *
 * <p>Detects BOTH patterns:</p>
 * <ol>
 *   <li>Swap happens <strong>after</strong> the first hit (wrong → right).</li>
 *   <li>Swap happens <strong>before</strong> the first hit (delay = 0 Meteor trick).</li>
 * </ol>
 *
 * Logic:<br>
 * • We store the most recent swap (old-slot, new-slot, old-item, new-item, time).<br>
 * • On a LEFT_CLICK_BLOCK we look back ≤150 ms:<br>
 *   – if the swap turned a wrong tool into the right tool → +VL.<br>
 * • We also keep the old “after-click” check (wrong at click, swap right afterwards).<br>
 * • ≤80 ms ⇒ +35 VL, otherwise +25 VL.<br>
 * • 4 detections in 5 s ⇒ +40 bonus VL.
 */
public final class AutoTool extends ViolationModule implements Listener {

    public static final AutoTool INSTANCE = new AutoTool();
    private AutoTool() { super("AutoTool"); }

    /* ───────── config helpers ───────── */
    private int cfg(String k, int d) { return loadInt(k, d); }

    /* ───────── per-player state ───────── */

    @Data private static final class Swap {
        long time;
        int  fromSlot, toSlot;
        ItemStack fromItem, toItem;
    }
    @Data private static final class Click {
        long time;
        Material block;
        int slot;
    }

    private static final Map<UUID, Swap>  LAST_SWAP  = new ConcurrentHashMap<>();
    private static final Map<UUID, Click> LAST_CLICK = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> STREAK   = new ConcurrentHashMap<>();
    private static final Map<UUID, Long>    STREAK_T = new ConcurrentHashMap<>();

    /* ───────── events ───────── */

    @EventHandler(ignoreCancelled = true)
    public void onSlot(PlayerItemHeldEvent e) {
        Swap s = new Swap();
        s.time      = System.currentTimeMillis();
        s.fromSlot  = e.getPreviousSlot();
        s.toSlot    = e.getNewSlot();
        s.fromItem  = e.getPlayer().getInventory().getItem(e.getPreviousSlot());
        s.toItem    = e.getPlayer().getInventory().getItem(e.getNewSlot());
        LAST_SWAP.put(e.getPlayer().getUniqueId(), s);
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeftClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_BLOCK || e.getClickedBlock() == null) return;

        UUID id = e.getPlayer().getUniqueId();
        long now = System.currentTimeMillis();

        /* store click for “swap-after” path */
        Click c = new Click();
        c.time  = now;
        c.block = e.getClickedBlock().getType();
        c.slot  = e.getPlayer().getInventory().getHeldItemSlot();
        LAST_CLICK.put(id, c);

        /* handle “swap-before-click” path */
        Swap s = LAST_SWAP.get(id);
        if (s == null) return;

        long delay = now - s.time;                           // swap → click
        int maxMs = cfg(".min_switch_delay", 150);
        if (delay < 0 || delay > maxMs) return;              // swap not close enough

        evaluateSuspicion(e.getPlayer().getInventory().getItem(s.fromSlot), // wrong?
                          e.getPlayer().getInventory().getItem(s.toSlot),   // right?
                          e.getClickedBlock().getType(),
                          delay, e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onLateSlot(PlayerItemHeldEvent e) {
        /* handle “swap-after-click” path */
        Click c = LAST_CLICK.get(e.getPlayer().getUniqueId());
        if (c == null) return;

        long now   = System.currentTimeMillis();
        long delay = now - c.time;                            // click → swap
        int maxMs  = cfg(".min_switch_delay", 150);
        if (delay > maxMs) return;

        ItemStack oldItem = e.getPlayer().getInventory().getItem(c.slot);
        ItemStack newItem = e.getPlayer().getInventory().getItem(e.getNewSlot());

        evaluateSuspicion(oldItem, newItem, c.block, delay, e.getPlayer());
    }

    /* ───────── core evaluation ───────── */

    private void evaluateSuspicion(ItemStack wrongTool, ItemStack rightTool,
                                   Material block, long delay, org.bukkit.entity.Player p) {

        if (wrongTool == null || rightTool == null) return;
        if (!isCorrectTool(block, rightTool)) return;          // new tool not correct
        if (isCorrectTool(block, wrongTool)) return;           // old tool was already fine

        User user = User.getUser(p);
        if (User.isUserInvalid(user, this)) return;
        if (!PingProvider.INSTANCE.atMostMaxPing(p, cfg(".max_ping", 400))) return;

        int add = (delay <= 80) ? 35 : 25;

        long win = cfg(".streak_window", 5000);
        UUID id  = p.getUniqueId();
        long now = System.currentTimeMillis();
        if (now - STREAK_T.getOrDefault(id, 0L) <= win) {
            int s = STREAK.merge(id, 1, Integer::sum);
            if (s >= 4) add += 40;
        } else {
            STREAK.put(id, 1);
        }
        STREAK_T.put(id, now);

        int cancelVl = cfg(".cancel_vl", 60);

        getManagement().flag(
            Flag.of(user)
                .setAddedVl(add)
                .setCancelAction(cancelVl, () -> {
                    InventoryUtil.syncUpdateInventory(p);
                    user.getTimeMap().at(TimeKey.AUTOTOOL_TIMEOUT).update();
                })
        );
    }

    /* ───────── helper ───────── */

    private static boolean isCorrectTool(Material block, ItemStack tool) {
        if (tool == null) return false;

        Material t = tool.getType();
        String b   = block.name();

        if (t == Material.SHEARS)
            return b.contains("WOOL") || b.contains("LEAVES") || b.equals("COBWEB");

        boolean axe    = t.name().endsWith("_AXE");
        boolean pick   = t.name().endsWith("_PICKAXE");
        boolean shovel = t.name().endsWith("_SHOVEL");
        boolean hoe    = t.name().endsWith("_HOE");

        if (axe    && (b.endsWith("_LOG") || b.contains("WOOD")   || b.contains("BAMBOO"))) return true;
        if (pick   && (b.contains("STONE") || b.contains("ORE")   || b.equals("ANCIENT_DEBRIS"))) return true;
        if (shovel && (b.contains("DIRT")  || b.contains("SAND")  || b.contains("GRAVEL") || b.contains("SNOW"))) return true;
        if (hoe    && (b.contains("HAY")   || b.contains("CROP")  || b.contains("WART")))  return true;

        return false;
    }

    /* ───────── violation management ───────── */

    @Override
    protected ViolationManagement createViolationManagement() {
        return ViolationLevelManagement.builder(this)
                                       .loadThresholdsToManagement()
                                       .withDecay(6000L, 15)
                                       .build();
    }
}

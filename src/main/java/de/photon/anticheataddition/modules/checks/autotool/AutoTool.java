package de.photon.anticheataddition.modules.checks.autotool;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.minecraft.ping.PingProvider;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AutoTool – detects suspicious tool swaps,
 * including the “Switch Back” option.
 */
public final class AutoTool extends ViolationModule implements Listener {

    public static final AutoTool INSTANCE = new AutoTool();
    private AutoTool() { super("AutoTool"); }

    /* ───────── per-player scratch data ───────── */

    private record Swap(long time, int fromSlot, int toSlot,
                        ItemStack fromItem, ItemStack toItem) {}
    private record Click(long time, Location loc, Material block,
                         int slot, ItemStack heldAtClick) {}
    private record Data(Swap lastSwap, Click lastClick,
                        long digStart,
                        int streak, long streakStart,
                        long lastCorrectSwapTime, int originalSlot) {}

    private static final Map<User, Data> STATE = new ConcurrentHashMap<>();

    private int cfg(String k, int d) { return loadInt(k, d); }

    /* ───────── events ───────── */

    @EventHandler(ignoreCancelled = true)
    public void onHotbarSwap(PlayerItemHeldEvent e) {
        User u = User.getUser(e.getPlayer());
        if (User.isUserInvalid(u, this)) return;

        /* cancel-VL timeout (inactive when cancel_vl = 0) */
        if (cfg(".cancel_vl", 0) > 0 &&
            u.getTimeMap().at(TimeKey.AUTOTOOL_TIMEOUT).recentlyUpdated(cfg(".timeout", 1500))) {
            e.setCancelled(true);
            InventoryUtil.syncUpdateInventory(e.getPlayer());
            return;
        }

        long now = System.currentTimeMillis();

        Data d = STATE.get(u);
        if (d != null && d.lastCorrectSwapTime > 0) {
            int backDelay = cfg(".back_switch_delay", 150);
            if (now - d.lastCorrectSwapTime <= backDelay &&
                e.getNewSlot() == d.originalSlot) {
                addBackViolation(u, e.getPlayer());
                d = new Data(d.lastSwap, d.lastClick,
                             d.digStart, d.streak, d.streakStart,
                             0, -1);
            }
        }

        Swap s = new Swap(now,
                e.getPreviousSlot(), e.getNewSlot(),
                e.getPlayer().getInventory().getItem(e.getPreviousSlot()),
                e.getPlayer().getInventory().getItem(e.getNewSlot()));

        if (d == null) d = new Data(null,null,0,0,0,0,-1);
        STATE.put(u, new Data(s, d.lastClick, d.digStart,
                              d.streak, d.streakStart,
                              d.lastCorrectSwapTime, d.originalSlot));
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeftClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_BLOCK || e.getClickedBlock() == null) return;

        User u = User.getUser(e.getPlayer());
        if (User.isUserInvalid(u, this)) return;

        ItemStack held = e.getPlayer().getInventory()
                          .getItem(e.getPlayer().getInventory().getHeldItemSlot());

        Click c = new Click(System.currentTimeMillis(),
                            e.getClickedBlock().getLocation(),
                            e.getClickedBlock().getType(),
                            e.getPlayer().getInventory().getHeldItemSlot(),
                            held);

        /* new dig session ALWAYS on mouse-down */
        STATE.put(u, new Data(null, c, c.time(), 0, 0, 0, -1));

        evaluateBeforeHit(u, e.getPlayer(), c);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapAfterClick(PlayerItemHeldEvent e) {
        User u = User.getUser(e.getPlayer());
        if (User.isUserInvalid(u, this)) return;

        Data d = STATE.get(u);
        if (d == null || d.lastClick == null) return;

        long now   = System.currentTimeMillis();
        long delay = now - d.lastClick.time();
        int  min   = cfg(".min_switch_delay", 150);
        if (delay > min) return;
        if (now - d.digStart >= min) return;           // late correction

        ItemStack was = d.lastClick.heldAtClick();
        ItemStack nowItem = e.getPlayer().getInventory().getItem(e.getNewSlot());

        evaluateSuspicion(u, e.getPlayer(), d,
                was, nowItem, delay, d.lastClick.block(), d.lastClick.slot());
    }

    /* swap-before-hit */
    private void evaluateBeforeHit(User u, org.bukkit.entity.Player p, Click c) {
        Data d = STATE.get(u);
        if (d == null || d.lastSwap == null) return;

        long delay = c.time() - d.lastSwap.time();
        int  min   = cfg(".min_switch_delay", 150);
        if (delay < 0 || delay > min) return;
        if (d.lastSwap.time() - d.digStart >= min) return;  // late correction

        evaluateSuspicion(u, p, d,
                d.lastSwap.fromItem(), d.lastSwap.toItem(),
                delay, c.block(), d.lastSwap.fromSlot());
    }

    /* ───────── core logic ───────── */

    private void evaluateSuspicion(User u, org.bukkit.entity.Player p, Data d,
                                   ItemStack wrongRaw, ItemStack right,
                                   long delay, Material block, int originalSlot) {

        if (right == null) return;                       // empty hand
        ItemStack wrong = wrongRaw == null ? new ItemStack(Material.AIR) : wrongRaw;

        if (!isCorrectTool(block, right)) return;        // swapped to wrong tool
        if (wrong.getType() != Material.AIR && isCorrectTool(block, wrong)) return; // both good
        if (!PingProvider.INSTANCE.atMostMaxPing(p, cfg(".max_ping", 400))) return;

        int add = (delay <= 80) ? 20 : 10;

        long now = System.currentTimeMillis();
        long win = cfg(".streak_window", 5000);
        int  st  = (now - d.streakStart <= win) ? d.streak + 1 : 1;
        long ss  = (st == 1) ? now : d.streakStart;
        if (st >= 4) add += 30;

        STATE.put(u, new Data(d.lastSwap, d.lastClick, d.digStart, st, ss,
                              now, originalSlot));

        int cancelVl = cfg(".cancel_vl", 200);

        getManagement().flag(
            Flag.of(u)
                .setAddedVl(add)
                .setCancelAction(cancelVl, () -> {
                    InventoryUtil.syncUpdateInventory(p);
                    u.getTimeMap().at(TimeKey.AUTOTOOL_TIMEOUT).update();
                })
        );
    }

    private void addBackViolation(User u, org.bukkit.entity.Player p) {
        getManagement().flag(Flag.of(u).setAddedVl(10));
    }

    /* tool matcher */
    private static boolean isCorrectTool(Material block, ItemStack tool) {
        if (tool == null) return false;

        Material t = tool.getType();
        String b = block.name();

        if (t == Material.SHEARS)
            return b.contains("LEAVES") || b.contains("WOOL") || b.equals("COBWEB");

        if (t.name().endsWith("_SWORD"))
            return b.equals("BAMBOO") || b.equals("BAMBOO_SHOOT");

        boolean axe    = t.name().endsWith("_AXE");
        boolean pick   = t.name().endsWith("_PICKAXE");
        boolean shovel = t.name().endsWith("_SHOVEL");
        boolean hoe    = t.name().endsWith("_HOE");

        if (pick && (b.contains("STONE") || b.contains("DEEPSLATE") || b.contains("ORE")
                     || b.contains("TERRACOTTA") || b.endsWith("_BLOCK")
                     || b.equals("OBSIDIAN") || b.equals("CRYING_OBSIDIAN")
                     || b.equals("NETHERRACK") || b.equals("END_STONE")
                     || (b.startsWith("RAW_") && b.endsWith("_BLOCK"))
                     || b.equals("ANCIENT_DEBRIS"))) return true;

        if (axe && (b.contains("WOOD") || b.endsWith("_LOG") || b.contains("PLANKS")
                    || b.contains("BAMBOO") || b.contains("CHEST") || b.equals("BARREL")
                    || b.contains("BOOKSHELF") || b.equals("LADDER")
                    || b.contains("SIGN") || b.contains("CAMPFIRE")
                    || b.equals("NOTE_BLOCK") || b.endsWith("_TABLE"))) return true;

        if (shovel && (b.contains("DIRT") || b.contains("GRAVEL") || b.contains("SAND")
                       || b.contains("SNOW") || b.contains("MUD") || b.contains("CLAY")
                       || b.equals("GRASS_BLOCK") || b.equals("PODZOL") || b.equals("ROOTED_DIRT")
                       || b.endsWith("CONCRETE_POWDER") || b.equals("SOUL_SAND") || b.equals("SOUL_SOIL")))
            return true;

        if (hoe && (b.contains("HAY") || b.contains("CROP") || b.contains("WART")
                    || b.contains("LEAVES") || b.contains("MOSS") || b.equals("DRIED_KELP_BLOCK")
                    || b.equals("TARGET"))) return true;

        return false;
    }

    @Override protected ViolationManagement createViolationManagement() {
        return ViolationLevelManagement.builder(this)
                                       .loadThresholdsToManagement()
                                       .withDecay(2400L, 30)
                                       .build();
    }
}

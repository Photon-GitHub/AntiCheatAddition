package de.photon.anticheataddition.modules.checks.autotool;

import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.minecraft.ping.PingProvider;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * AutoTool – detects Meteor-style tool swaps,
 * including the “Switch Back” option.
 */
public final class AutoTool extends ViolationModule implements Listener {

    public static final AutoTool INSTANCE = new AutoTool();
    private AutoTool() { super("AutoTool"); }

    /* ───────── per-player scratch data ───────── */

    private record Swap(long time, int fromSlot, int toSlot,
                        ItemStack fromItem, ItemStack toItem) {}
    private record Click(long time, Material block, int slot,
                         ItemStack heldAtClick) {}
    private record Data(Swap lastSwap, Click lastClick,
                        int streak, long streakStart,
                        long lastCorrectSwapTime, int originalSlot) {}

    private static final Map<User, Data> STATE = new ConcurrentHashMap<>();

    private int cfg(String k, int d) { return loadInt(k, d); }

    /* ───────── events ───────── */

    @EventHandler(ignoreCancelled = true)
    public void onHotbarSwap(PlayerItemHeldEvent e) {
        User u = User.getUser(e.getPlayer());
        if (User.isUserInvalid(u, this)) return;

        long now  = System.currentTimeMillis();
        int  newS = e.getNewSlot();

        Data d = STATE.get(u);
        /* detect instant “switch back” */
        if (d != null && d.lastCorrectSwapTime > 0) {
            int backDelay = cfg(".back_switch_delay", 150);
            if (now - d.lastCorrectSwapTime <= backDelay && newS == d.originalSlot) {
                addBackViolation(u, e.getPlayer());
                d = new Data(d.lastSwap, d.lastClick,
                             d.streak, d.streakStart,
                             0, -1);                       // reset timer
            }
        }

        Swap s = new Swap(now,
                e.getPreviousSlot(), newS,
                e.getPlayer().getInventory().getItem(e.getPreviousSlot()),
                e.getPlayer().getInventory().getItem(newS));

        if (d == null) d = new Data(null, null, 0, 0, 0, -1);
        STATE.put(u, new Data(s, d.lastClick, d.streak, d.streakStart,
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
                            e.getClickedBlock().getType(),
                            e.getPlayer().getInventory().getHeldItemSlot(),
                            held);

        Data d = STATE.getOrDefault(u, new Data(null,null,0,0,0,-1));
        STATE.put(u, new Data(d.lastSwap, c, d.streak, d.streakStart,
                              d.lastCorrectSwapTime, d.originalSlot));

        evaluateBeforeHit(u, e.getPlayer(), c);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwapAfterClick(PlayerItemHeldEvent e) {
        User u = User.getUser(e.getPlayer());
        if (User.isUserInvalid(u, this)) return;

        Data d = STATE.get(u);
        if (d == null || d.lastClick == null) return;

        long delay = System.currentTimeMillis() - d.lastClick.time();
        if (delay > cfg(".min_switch_delay", 150)) return;

        ItemStack was = d.lastClick.heldAtClick();
        ItemStack now = e.getPlayer().getInventory().getItem(e.getNewSlot());

        evaluateSuspicion(u, e.getPlayer(), d,
                was, now, delay, d.lastClick.block(), d.lastClick.slot());
    }

    /* swap-before-hit */
    private void evaluateBeforeHit(User u, org.bukkit.entity.Player p, Click c) {
        Data d = STATE.get(u);
        if (d == null || d.lastSwap == null) return;

        long delay = c.time() - d.lastSwap.time();
        if (delay < 0 || delay > cfg(".min_switch_delay", 150)) return;

        evaluateSuspicion(u, p, d,
                d.lastSwap.fromItem(), d.lastSwap.toItem(),
                delay, c.block(), d.lastSwap.fromSlot());
    }

    /* ───────── core logic ───────── */

    private void evaluateSuspicion(User u, org.bukkit.entity.Player p, Data d,
                                   ItemStack wrongRaw, ItemStack right,
                                   long delay, Material block, int originalSlot) {

        if (right == null) return;
        ItemStack wrong = wrongRaw == null ? new ItemStack(Material.AIR) : wrongRaw;

        if (!isCorrectTool(block, right)) return;
        if (wrong.getType() != Material.AIR && isCorrectTool(block, wrong)) return;
        if (!PingProvider.INSTANCE.atMostMaxPing(p, cfg(".max_ping", 400))) return;

        /* base VLs */
        int add = (delay <= 80) ? 20 : 10;

        long now = System.currentTimeMillis();
        long win = cfg(".streak_window", 5000);
        int  st  = (now - d.streakStart <= win) ? d.streak + 1 : 1;
        long ss  = (st == 1) ? now : d.streakStart;
        if (st >= 4) add += 30;

        STATE.put(u, new Data(d.lastSwap, d.lastClick, st, ss,
                              now, originalSlot));

        int cancelVl = cfg(".cancel_vl", 60);

        getManagement().flag(
            Flag.of(u)
                .setAddedVl(add)
                .setCancelAction(cancelVl, () -> {
                    InventoryUtil.syncUpdateInventory(p);
                    u.getTimeMap().at(TimeKey.AUTOTOOL_TIMEOUT).update();
                })
        );
    }

    /* extra VL for instant switch-back */
    private void addBackViolation(User u, org.bukkit.entity.Player p) {
        getManagement().flag(Flag.of(u).setAddedVl(10));   // mild penalty
    }

    /* ───── tool matcher (unchanged) ───── */
    private static boolean isCorrectTool(Material block, ItemStack tool) {
        if (tool == null) return false;

        Material t = tool.getType();
        String b = block.name();

        if (t == Material.SHEARS)
            return b.contains("LEAVES") || b.contains("WOOL") || b.equals("COBWEB");

        if (t.name().endsWith("_SWORD"))
            return b.equals("BAMBOO") || b.equals("BAMBOO_SHOOT");

        boolean axe = t.name().endsWith("_AXE");
        boolean pick = t.name().endsWith("_PICKAXE");
        boolean shovel = t.name().endsWith("_SHOVEL");
        boolean hoe = t.name().endsWith("_HOE");

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

    /* ───── violation mgmt ───── */
    @Override protected ViolationManagement createViolationManagement() {
        return ViolationLevelManagement.builder(this)
                                       .loadThresholdsToManagement()
                                       .withDecay(6000L, 15)
                                       .build();
    }
}
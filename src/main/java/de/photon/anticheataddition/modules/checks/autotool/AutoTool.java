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
import lombok.Data;
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
 * AutoTool check designed to catch Meteor-style “smart-tool” mods.
 *
 * <p>Flags when the player starts mining with a <i>wrong</i> item and,
 * within ≤ min_switch_delay ms, hot-bar-switches to the correct tool.</p>
 *
 * VL logic:  ≤50 ms → +30 ◆ ≤100 ms → +20 ◆ ≤min_switch_delay → +10.  
 * Three detections inside buffer_decay adds +20 bonus VL.
 */
public final class AutoTool extends ViolationModule implements Listener {

    public static final AutoTool INSTANCE = new AutoTool();

    private AutoTool() { super("AutoTool"); }

    /* ───────────────────────────────── data ───────────────────────────────── */

    @Data
    private static final class ClickInfo {
        private long     time;      // ms
        private Material blockType; // material clicked
        private int      slot;      // slot held on click
    }

    /** last LEFT_CLICK_BLOCK per player */
    private static final Map<UUID, ClickInfo> LAST_CLICK = new ConcurrentHashMap<>();

    /** pattern buffer (3 detections inside buffer_decay) */
    private static final Map<UUID, Long>    LAST_DETECTION = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> STREAK         = new ConcurrentHashMap<>();

    /* ───────────────────────── config helper ───────────────────────── */

    private int cfg(String path, int def) { return loadInt(path, def); }

    /* ─────────────────────────── events ─────────────────────────── */

    @EventHandler(ignoreCancelled = true)
    public void onLeftClick(PlayerInteractEvent e) {
        if (e.getAction() != Action.LEFT_CLICK_BLOCK || e.getClickedBlock() == null) return;

        ClickInfo info = new ClickInfo();
        info.setTime(System.currentTimeMillis());
        info.setBlockType(e.getClickedBlock().getType());
        info.setSlot(e.getPlayer().getInventory().getHeldItemSlot());

        LAST_CLICK.put(e.getPlayer().getUniqueId(), info);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHotbarSwitch(PlayerItemHeldEvent e) {

        /* quick global exemptions */
        User user = User.getUser(e.getPlayer());
        if (User.isUserInvalid(user, this)) return;
        if (!TPSProvider.INSTANCE.atLeastTPS(19)) return;
        if (!PingProvider.INSTANCE.atMostMaxPing(e.getPlayer(), cfg(".max_ping", 400))) return;

        /* timeout after cancel_vl breach */
        if (user.getTimeMap().at(TimeKey.AUTOTOOL_TIMEOUT)
                .recentlyUpdated(cfg(".timeout", 3000))) {
            e.setCancelled(true);
            return;
        }

        ClickInfo click = LAST_CLICK.get(e.getPlayer().getUniqueId());
        if (click == null) return;

        long delay = System.currentTimeMillis() - click.getTime();
        int minDelay = cfg(".min_switch_delay", 150);
        if (delay > minDelay) return;                    // swap too slow

        ItemStack oldItem = e.getPlayer().getInventory().getItem(click.getSlot());
        ItemStack newItem = e.getPlayer().getInventory().getItem(e.getNewSlot());

        boolean oldGood = isCorrectTool(click.getBlockType(), oldItem);
        boolean newGood = isCorrectTool(click.getBlockType(), newItem);
        if (oldGood || !newGood) return;                 // no suspicious change

        /* VL calculation */
        int addedVl = (delay <= 50) ? 30 : (delay <= 100) ? 20 : 10;

        long now = System.currentTimeMillis();
        long last = LAST_DETECTION.getOrDefault(user.getUuid(), 0L);
        if (now - last <= cfg(".buffer_decay", 4000)) {
            int streak = STREAK.merge(user.getUuid(), 1, Integer::sum);
            if (streak >= 3) addedVl += 20;
        } else {
            STREAK.put(user.getUuid(), 1);
        }
        LAST_DETECTION.put(user.getUuid(), now);

        int cancelVl = cfg(".cancel_vl", 60);

        getManagement().flag(
            Flag.of(user)
                .setAddedVl(addedVl)
                .setCancelAction(cancelVl, () -> {
                    e.setCancelled(true);
                    InventoryUtil.syncUpdateInventory(e.getPlayer());
                    user.getTimeMap().at(TimeKey.AUTOTOOL_TIMEOUT).update();
                })
        );
    }

    /* ────────────────────── tool / block matching ────────────────────── */

    private static boolean isCorrectTool(Material block, ItemStack tool) {
        if (tool == null) return false;

        Material m = tool.getType();
        String b = block.name();

        if (m == Material.SHEARS) {
            return b.contains("WOOL") || b.contains("LEAVES") || b.equals("COBWEB");
        }

        boolean axe    = m.name().endsWith("_AXE");
        boolean pick   = m.name().endsWith("_PICKAXE");
        boolean shovel = m.name().endsWith("_SHOVEL");
        boolean hoe    = m.name().endsWith("_HOE");

        if (axe    && (b.endsWith("_LOG") || b.contains("WOOD") || b.contains("BAMBOO")))        return true;
        if (pick   && (b.contains("STONE") || b.contains("ORE") || b.equals("ANCIENT_DEBRIS")))  return true;
        if (shovel && (b.contains("DIRT") || b.contains("SAND") || b.contains("GRAVEL") ||
                       b.contains("SNOW")))                                                      return true;
        if (hoe    && (b.contains("HAY")  || b.contains("CROP") || b.contains("WART")))          return true;

        return false;
    }

    /* ─────────────────── violation-management boilerplate ─────────────────── */

    @Override
    protected ViolationManagement createViolationManagement() {
        return ViolationLevelManagement.builder(this)
                                       .loadThresholdsToManagement()
                                       .withDecay(6000L, 20)
                                       .build();
    }
}

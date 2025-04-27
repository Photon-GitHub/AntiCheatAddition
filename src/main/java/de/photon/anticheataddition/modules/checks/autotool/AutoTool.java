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
 * AutoTool check aimed at Meteor-style “smart tool” mods.
 *
 * • Player starts mining with a wrong tool, then swaps to the right tool in ≤ min_switch_delay ms.<br>
 * • Adds VL depending on latency: ≤50 ms → +30│≤100 ms → +20│≤min_switch_delay → +10.<br>
 * • Three detections inside buffer_decay give a +20 bonus.<br>
 * • Cancel and timeout controlled by config keys.
 */
public final class AutoTool extends ViolationModule implements Listener {

    public static final AutoTool INSTANCE = new AutoTool();

    private AutoTool() { super("AutoTool"); }

    /* ───────────────────────── data holding ───────────────────────── */

    @Data
    private static final class ClickInfo {
        private long     time;
        private Material blockType;
        private int      slot;
    }

    /** last LEFT_CLICK_BLOCK per player */
    private static final Map<UUID, ClickInfo> LAST_CLICK     = new ConcurrentHashMap<>();
    /** pattern buffer */
    private static final Map<UUID, Long>      LAST_DETECTION = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer>   STREAK         = new ConcurrentHashMap<>();

    /* ───────────────────────── config helper ───────────────────────── */

    private int cfg(String key, int def) { return loadInt(key, def); }

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

        /* global exemptions */
        User user = User.getUser(e.getPlayer());
        if (User.isUserInvalid(user, this)) return;
        if (!TPSProvider.INSTANCE.atLeastTPS(19)) return;
        if (!PingProvider.INSTANCE.atMostMaxPing(e.getPlayer(), cfg(".max_ping", 400))) return;

        /* timeout after cancel_vl */
        if (user.getTimeMap().at(TimeKey.AUTOTOOL_TIMEOUT)
                .recentlyUpdated(cfg(".timeout", 3000))) {
            e.setCancelled(true);
            return;
        }

        ClickInfo click = LAST_CLICK.get(e.getPlayer().getUniqueId());
        if (click == null) return;

        long delay    = System.currentTimeMillis() - click.getTime();
        int  minDelay = cfg(".min_switch_delay", 150);
        if (delay > minDelay) return;

        ItemStack oldItem = e.getPlayer().getInventory().getItem(click.getSlot());
        ItemStack newItem = e.getPlayer().getInventory().getItem(e.getNewSlot());

        boolean oldGood = isCorrectTool(click.getBlockType(), oldItem);
        boolean newGood = isCorrectTool(click.getBlockType(), newItem);
        if (oldGood || !newGood) return;

        /* VL calculation */
        int addedVl = (delay <= 50) ? 30 : (delay <= 100) ? 20 : 10;

        UUID id   = user.getPlayer().getUniqueId();
        long now  = System.currentTimeMillis();
        long last = LAST_DETECTION.getOrDefault(id, 0L);

        if (now - last <= cfg(".buffer_decay", 4000)) {
            int streak = STREAK.merge(id, 1, Integer::sum);
            if (streak >= 3) addedVl += 20;
        } else {
            STREAK.put(id, 1);
        }
        LAST_DETECTION.put(id, now);

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

    /* ─────────────── tool ↔ block matching ─────────────── */

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

    /* ─────────── violation-management boilerplate ─────────── */

    @Override
    protected ViolationManagement createViolationManagement() {
        return ViolationLevelManagement.builder(this)
                                       .loadThresholdsToManagement()
                                       .withDecay(6000L, 20)
                                       .build();
    }
}

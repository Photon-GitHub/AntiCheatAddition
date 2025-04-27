package de.photon.anticheataddition.modules.checks.autotool;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerCommon;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.interceptor.InterceptorPriority;
import com.github.retrooper.packetevents.packet.play.in.*;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
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
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Extremely aggressive AutoTool detection – catches Meteor in 2–3 swings.
 */
public final class AutoTool extends ViolationModule implements Listener, PacketListenerCommon {

    public static final AutoTool INSTANCE = new AutoTool();

    private AutoTool() { super("AutoTool"); }

    /* ───────────────────────── config helpers ───────────────────────── */

    private int cfg(String key, int def) { return loadInt(key, def); }

    /* ───────────────────────── player state ───────────────────────── */

    @Data
    private static final class State {
        long lastFlying;           // last Flying packet (server tick boundary)
        long lastHeld;             // ts of HeldItemSlot
        long lastDig;              // ts of START_DESTROY_BLOCK
        int  lastHeldSlot;
        int  streak;               // suspicious swaps inside window
        long streakStart;
    }

    private static final Map<UUID, State> STATES = new ConcurrentHashMap<>();

    /* ───────────────────────── module loader ───────────────────────── */

    @Override
    protected ModuleLoader createModuleLoader() {
        return ModuleLoader.builder(this)
                           .build()
                           .addPacketListener(this, InterceptorPriority.NORMAL);
    }

    /* ───────────────────────── packets ───────────────────────── */

    @Override
    public void onPacketReceive(PacketReceiveEvent e) {

        if (!(e.getPlayer() instanceof org.bukkit.entity.Player bp)) return;
        User user = User.getUser(bp);
        if (User.isUserInvalid(user, this)) return;

        // ping gate
        if (!PingProvider.INSTANCE.atMostMaxPing(bp, cfg(".max_ping", 400))) return;

        State s = STATES.computeIfAbsent(bp.getUniqueId(), id -> new State());

        if (e.getPacketType() == PacketPlayInHeldItemSlot.TYPE) {
            PacketPlayInHeldItemSlot p = e.getPacket();
            s.lastHeld     = System.currentTimeMillis();
            s.lastHeldSlot = p.getSlot();
            return;
        }

        if (e.getPacketType() == PacketPlayInFlying.TYPE) {
            s.lastFlying = System.currentTimeMillis();
            decayStreak(s);
            return;
        }

        if (e.getPacketType() == PacketPlayInPlayerDigging.TYPE) {
            PacketPlayInPlayerDigging dig = e.getPacket();
            if (dig.getAction() != PlayerDiggingAction.START_DESTROY_BLOCK) return;

            long now = System.currentTimeMillis();
            // held packet must be within ±100 ms of dig AND after last flying < 10 ms (= same tick)
            if (Math.abs(now - s.lastHeld) > 100) return;
            if (now - s.lastFlying > 10) return;

            // evaluate tool correctness
            ItemStack oldItem = bp.getInventory().getItem(s.lastHeldSlot);
            ItemStack curItem = bp.getInventory().getItem(bp.getInventory().getHeldItemSlot());
            Block     b       = bp.getWorld().getBlockAt(dig.getBlockPosition().getX(),
                                                         dig.getBlockPosition().getY(),
                                                         dig.getBlockPosition().getZ());

            if (oldItem == null || curItem == null) return;
            if (isCorrectTool(b.getType(), oldItem)) return;      // already right tool → legit
            if (!isCorrectTool(b.getType(), curItem)) return;     // still wrong tool

            /* --------------- suspicious swap detected --------------- */
            int addedVl = 30;                                     // hard +30 every time

            long window = cfg(".streak_window", 5000);
            if (now - s.streakStart <= window) {
                if (++s.streak >= 4) addedVl += 50;               // avalanche
            } else {
                s.streak = 1;
                s.streakStart = now;
            }

            int cancelVl = cfg(".cancel_vl", 60);

            getManagement().flag(
                Flag.of(user)
                    .setAddedVl(addedVl)
                    .setCancelAction(cancelVl, () -> {
                        InventoryUtil.syncUpdateInventory(bp);
                        user.getTimeMap().at(TimeKey.AUTOTOOL_TIMEOUT).update();
                    })
            );
        }
    }

    /* ───────────────────────── Bukkit housekeeping ───────────────────────── */

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        STATES.remove(e.getPlayer().getUniqueId());
    }

    /* ───────────────────────── utilities ───────────────────────── */

    private static void decayStreak(State s) {
        // drop streak by 1 every flying packet to emulate slow decay (~10 VL / min)
        if (s.streak > 0) s.streak--;
    }

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

    /* ───────────────── violation-management boilerplate ───────────────── */

    @Override
    protected ViolationManagement createViolationManagement() {
        // very slow decay (10 VL/minute equivalent via refresh)
        return ViolationLevelManagement.builder(this)
                                       .loadThresholdsToManagement()
                                       .withDecay(6000L, 10)
                                       .build();
    }
}

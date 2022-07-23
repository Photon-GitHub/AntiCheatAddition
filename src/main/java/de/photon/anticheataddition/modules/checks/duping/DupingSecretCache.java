package de.photon.anticheataddition.modules.checks.duping;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.minecraft.world.MaterialUtil;
import de.photon.anticheataddition.util.minecraft.world.WorldUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

public final class DupingSecretCache extends ViolationModule implements Listener
{
    public static final DupingSecretCache INSTANCE = new DupingSecretCache();
    private final long secretCacheCheckDelayTicks = 20L * 60L * loadLong("check_delay", 10); // minutes to ticks

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event)
    {
        switch (event.getInventory().getType()) {
            // Check only chests and shulker boxes to prevent false positives.
            case CHEST, SHULKER_BOX -> {
                val user = User.getUser(event.getPlayer().getUniqueId());
                if (User.isUserInvalid(user, this) || event.getInventory().isEmpty()) return;

                // Artificial plugin inventories might not have a location.
                val loc = event.getInventory().getLocation();
                if (loc == null) return;

                val block = loc.getBlock();

                // Check after x minutes how many blocks surround the chest or shulker box.
                // If the chest or shulker box is completely surrounded, flag as secret cache.
                Bukkit.getScheduler().scheduleSyncDelayedTask(AntiCheatAddition.getInstance(), () -> {
                    final long surroundingBlocks = WorldUtil.INSTANCE.countBlocksAround(block, WorldUtil.ALL_FACES, MaterialUtil.LIQUIDS);

                    // Secret cache if surrounded on all sides.
                    if (surroundingBlocks == WorldUtil.ALL_FACES.size()) {
                        getManagement().flag(Flag.of(user).setAddedVl(60));
                    }
                }, secretCacheCheckDelayTicks);
            }
        }
    }

    private DupingSecretCache()
    {
        super("Duping.parts.SecretCache");
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).loadThresholdsToManagement().withDecay(18000L, 30).build();
    }
}

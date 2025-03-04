package de.photon.anticheataddition.modules.checks.duping;

import com.tcoded.folialib.FoliaLib;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.ServerVersion;
import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.datastructure.SetUtil;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.mathematics.TimeUtil;
import de.photon.anticheataddition.util.minecraft.world.WorldUtil;
import de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class DupingSecretCache extends ViolationModule implements Listener
{
    public static final DupingSecretCache INSTANCE = new DupingSecretCache();
    private static final Set<Material> IGNORED_AROUND_INVENTORY = Stream.of(MaterialUtil.INSTANCE.getLiquids(), MaterialUtil.INSTANCE.getFreeSpaceContainers())
                                                                        .flatMap(Set::stream)
                                                                        .collect(SetUtil.toImmutableEnumSet());

    private final long secretCacheCheckDelayTicks = TimeUtil.toTicks(loadLong(".check_delay", 10), TimeUnit.MINUTES);

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event)
    {
        switch (event.getInventory().getType()) {
            // Check only chests and shulker boxes to prevent false positives.
            case CHEST, SHULKER_BOX -> {
                final var user = User.getUser(event.getPlayer().getUniqueId());
                // Use .iterator().hasNext() to check if the inventory is empty, instead of isEmpty() as that method is not available on 1.8.8.
                if (User.isUserInvalid(user, this) || !event.getInventory().iterator().hasNext()) return;

                // Artificial plugin inventories might not have a location.
                final var loc = event.getInventory().getLocation();
                if (loc == null) return;

                // Make sure that opening an inventory twice does not trigger two violations.
                if (user.getData().object.dupingSecretCacheCurrentlyCheckedLocations.add(loc)) {
                    final var block = loc.getBlock();
                    final Material oldMaterial = block.getType();

                    Log.finer(() -> "Checking secret cache for " + user.getPlayer().getName() +
                                    " at " + block.getX() + " " + block.getY() + " " + block.getZ() +
                                    " in " + secretCacheCheckDelayTicks + " ticks.");

                    // Check after x minutes how many blocks surround the chest or shulker box.
                    // If the chest or shulker box is completely surrounded, flag as secret cache.
                    FoliaLib foliaLib = AntiCheatAddition.getInstance().getFoliaLib();
                    if (foliaLib.isFolia()) {
                        foliaLib.getScheduler().runAtLocationLater(
                                loc,
                                task -> {
                                    if (user.getPlayer() == null || !user.getPlayer().isOnline()) return;
                                    if (loc.getBlock().getType() != oldMaterial) return;

                                    user.getData().object.dupingSecretCacheCurrentlyCheckedLocations.remove(loc);

                                    final long surroundingBlocks = WorldUtil.INSTANCE.countBlocksAround(block, WorldUtil.ALL_FACES, IGNORED_AROUND_INVENTORY);

                                    Log.finer(() -> "Surrounding blocks for secret cache of player " + user.getPlayer().getName() + " : " + surroundingBlocks + " | Needed for flag: " + WorldUtil.ALL_FACES.size());

                                    if (surroundingBlocks == WorldUtil.ALL_FACES.size()) {
                                        getManagement().flag(Flag.of(user)
                                                .setAddedVl(50)
                                                .setDebug(() -> "Identified secret cache of player " + user.getPlayer().getName() +
                                                        "of type " + oldMaterial +
                                                        " at " + block.getX() + " " + block.getY() + " " + block.getZ() +
                                                        " in world " + block.getWorld().getName()));
                                    }
                                },
                                secretCacheCheckDelayTicks * 50,
                                TimeUnit.MILLISECONDS
                        );
                    } else {
                        Bukkit.getScheduler().runTaskLater(AntiCheatAddition.getInstance(), () -> {
                            // Now that the location is checked, allow queueing it again.
                            user.getData().object.dupingSecretCacheCurrentlyCheckedLocations.remove(loc);


                            // Block has not changed.
                            if (loc.getBlock().getType() != oldMaterial) return;

                            final long surroundingBlocks = WorldUtil.INSTANCE.countBlocksAround(block, WorldUtil.ALL_FACES, IGNORED_AROUND_INVENTORY);

                            Log.finer(() -> "Surrounding blocks for secret cache of player " + user.getPlayer().getName() + " : " + surroundingBlocks + " | Needed for flag: " + WorldUtil.ALL_FACES.size());

                            // Secret cache if surrounded on all sides.
                            if (surroundingBlocks == WorldUtil.ALL_FACES.size()) {
                                getManagement().flag(Flag.of(user).setAddedVl(50).setDebug(() -> "Identified secret cache of player " + user.getPlayer().getName() +
                                        "of type " + oldMaterial +
                                        " at " + block.getX() + " " + block.getY() + " " + block.getZ() +
                                        " in world " + block.getWorld().getName()));
                            }
                        }, secretCacheCheckDelayTicks);
                    }
                }
            }
        }
    }

    private DupingSecretCache()
    {
        super("Duping.parts.SecretCache");
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .setAllowedServerVersions(ServerVersion.NON_188_VERSIONS)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).emptyThresholdManagement().withDecay(18000L, 30).build();
    }
}

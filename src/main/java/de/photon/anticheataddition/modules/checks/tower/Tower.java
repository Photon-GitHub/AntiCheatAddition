package de.photon.anticheataddition.modules.checks.tower;

import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.user.data.batch.TowerBatch;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.minecraft.world.MaterialUtil;
import de.photon.anticheataddition.util.minecraft.world.WorldUtil;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;


public final class Tower extends ViolationModule implements Listener
{
    public static final Tower INSTANCE = new Tower();

    private final int timeout = loadInt(".timeout", 6000);

    private Tower()
    {
        super("Tower");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        // To prevent too fast towering -> Timeout
        if (user.getTimeMap().at(TimeKey.TOWER_TIMEOUT).recentlyUpdated(timeout)) {
            event.setCancelled(true);
            InventoryUtil.syncUpdateInventory(user.getPlayer());
            return;
        }

        // Not flying
        if (!user.getPlayer().isFlying()) {
            val blockPlaced = event.getBlockPlaced();

            // User must stand above the block (placed from above)1
            // Check if the block is tower-placed (Block belows)
            if (event.getBlock().getFace(event.getBlockAgainst()) == BlockFace.DOWN &&
                // The block is placed inside a 2 - block y-radius, this prevents false positives when building from a higher level
                user.getPlayer().getLocation().getY() - blockPlaced.getY() < 2D &&
                // Check if this check applies to the block
                blockPlaced.getType().isSolid() &&
                //
                // Custom formula when setting -> Will return negative value when in protected timeframe.
                user.getTimeMap().at(TimeKey.TOWER_BOUNCE).passedTime() > 0 &&
                // Check if the block is placed against only one block (face).
                // Only one block that is not a liquid is allowed (the one which the Block is placed against).
                WorldUtil.INSTANCE.countBlocksAround(blockPlaced, WorldUtil.ALL_FACES, MaterialUtil.LIQUIDS) == 1 &&
                // User is not in water which can cause false positives due to faster swimming on newer versions.
                !user.isInLiquids())
            {
                // Make sure that the player is still towering in the same position.
                if (!event.getBlockAgainst().getLocation().equals(user.getTowerBatch().peekLastAdded().locationOfBlock())) user.getTowerBatch().clear();

                user.getTowerBatch().addDataPoint(new TowerBatch.TowerBlockPlace(blockPlaced.getLocation(), user.getPlayer()));
            }
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val batchProcessor = new TowerBatchProcessor(this);
        return ModuleLoader.builder(this)
                           .batchProcessor(batchProcessor)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).loadThresholdsToManagement().withDecay(120L, 15).build();
    }
}
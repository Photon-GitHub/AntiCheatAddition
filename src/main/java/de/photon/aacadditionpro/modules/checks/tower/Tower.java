package de.photon.aacadditionpro.modules.checks.tower;

import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.user.data.batch.TowerBatch;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.inventory.InventoryUtil;
import de.photon.aacadditionpro.util.minecraft.world.MaterialUtil;
import de.photon.aacadditionpro.util.minecraft.world.WorldUtil;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;


public class Tower extends ViolationModule implements Listener
{
    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    public Tower()
    {
        super("Tower");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        // To prevent too fast towering -> Timeout
        if (user.getTimestampMap().at(TimestampKey.TOWER_TIMEOUT).recentlyUpdated(timeout)) {
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
                user.getTimestampMap().at(TimestampKey.TOWER_BOUNCE).passedTime() > 0 &&
                // Check if the block is placed against only one block (face).
                // Only one block that is not a liquid is allowed (the one which the Block is placed against).
                WorldUtil.INSTANCE.countBlocksAround(blockPlaced, WorldUtil.ALL_FACES, MaterialUtil.LIQUIDS) == 1 &&
                // User is not in water which can cause false positives due to faster swimming on newer versions.
                !user.isInLiquids())
            {
                // Make sure that the player is still towering in the same position.
                if (!event.getBlockAgainst().getLocation().equals(user.getTowerBatch().peekLastAdded().getLocationOfBlock())) user.getTowerBatch().clear();

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
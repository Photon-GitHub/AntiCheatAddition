package de.photon.aacadditionpro.modules.checks.tower;

import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.user.data.batch.TowerBatch;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.inventory.InventoryUtil;
import de.photon.aacadditionpro.util.mathematics.Hitbox;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import de.photon.aacadditionpro.util.world.BlockUtil;
import de.photon.aacadditionpro.util.world.InternalPotion;
import de.photon.aacadditionpro.util.world.MaterialUtil;
import lombok.Getter;
import lombok.val;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;


public class Tower extends ViolationModule implements Listener
{
    @Getter
    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancelVl;

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
                user.getTimestampMap().at(TimestampKey.TOWER_SLIME_JUMP).passedTime() > 0 &&
                // Check if the block is placed against one block (face) only
                // Only one block that is not a liquid is allowed (the one which the Block is placed against).
                BlockUtil.countBlocksAround(blockPlaced, BlockUtil.ALL_FACES, MaterialUtil.LIQUIDS) == 1 &&
                // User is not in water which can cause false positives due to faster swimming on newer versions.
                !MaterialUtil.containsLiquids(Hitbox.PLAYER.getPartiallyIncludedMaterials(user.getPlayer().getLocation())))
            {
                // Make sure that the player is still towering in the same position.
                if (!event.getBlockAgainst().getLocation().equals(user.getTowerBatch().peekLastAdded().getLocationOfBlock())) user.getTowerBatch().clear();

                user.getTowerBatch().addDataPoint(new TowerBatch.TowerBlockPlace(blockPlaced.getLocation(),
                                                                                 InternalPotion.JUMP.getPotionEffect(user.getPlayer()),
                                                                                 InternalPotion.LEVITATION.getPotionEffect(user.getPlayer())));
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
        return ViolationLevelManagement.builder(this).withDecay(120L, 1).build();
    }
}
package de.photon.aacadditionpro.modules.checks.tower;

import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import de.photon.aacadditionproold.user.TimestampKey;
import de.photon.aacadditionproold.user.User;
import de.photon.aacadditionproold.user.UserManager;
import de.photon.aacadditionproold.user.subdata.datawrappers.TowerBlockPlace;
import de.photon.aacadditionproold.util.entity.EntityUtil;
import de.photon.aacadditionproold.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionproold.util.inventory.InventoryUtils;
import de.photon.aacadditionproold.util.potion.InternalPotionEffectType;
import de.photon.aacadditionproold.util.potion.PotionUtil;
import de.photon.aacadditionproold.util.world.BlockUtils;
import lombok.Getter;
import lombok.val;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;


public class Tower extends ViolationModule implements Listener
{
    @Getter
    private static final Tower instance = new Tower();

    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 120L);

    @Getter
    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancelVl;

    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // To prevent too fast towering -> Timeout
        if (user.getTimestampMap().recentlyUpdated(TimestampKey.TOWER_TIMEOUT, timeout)) {
            event.setCancelled(true);
            InventoryUtils.syncUpdateInventory(user.getPlayer());
            return;
        }

        // Not flying
        if (!user.getPlayer().isFlying()) {
            final Block blockPlaced = event.getBlockPlaced();

            // Levitation effect
            final Integer levitation = PotionUtil.getAmplifier(PotionUtil.getPotionEffect(user.getPlayer(), InternalPotionEffectType.LEVITATION));

            // User must stand above the block (placed from above)1
            // Check if the block is tower-placed (Block belows)
            if (event.getBlock().getFace(event.getBlockAgainst()) == BlockFace.DOWN &&
                // The block is placed inside a 2 - block y-radius, this prevents false positives when building from a higher level
                user.getPlayer().getLocation().getY() - blockPlaced.getY() < 2D &&
                // Check if this check applies to the block
                blockPlaced.getType().isSolid() &&
                //
                // Custom formula when setting -> Will return negative value when in protected timeframe.
                user.getTimestampMap().passedTime(TimestampKey.TOWER_SLIME_JUMP) > 0 &&
                // Check if the block is placed against one block (face) only
                // Only one block that is not a liquid is allowed (the one which the Block is placed against).
                BlockUtils.countBlocksAround(blockPlaced, true) == 1 &&
                // User is not in water which can cause false positives due to faster swimming on newer versions.
                !EntityUtil.isHitboxInLiquids(user.getPlayer().getLocation(), user.getHitbox()))
            {
                // Make sure that the player is still towering in the same position.
                if (!event.getBlockAgainst().getLocation().equals(user.getTowerData().getBatch().peekLastAdded().getBlock().getLocation())) {
                    user.getTowerData().getBatch().clear();
                }

                user.getTowerData().getBatch().addDataPoint(
                        new TowerBlockPlace(
                                blockPlaced,
                                //Jump boost effect is important
                                PotionUtil.getAmplifier(PotionUtil.getPotionEffect(user.getPlayer(), InternalPotionEffectType.JUMP)),
                                levitation));

            }
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        val batchProcessor =
        return ModuleLoader.builder(this)
                           .batchProcessor()
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).withDecay(120L, 1).build();
    }
}
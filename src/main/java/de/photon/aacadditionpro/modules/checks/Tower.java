package de.photon.aacadditionpro.modules.checks;

import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.user.subdata.datawrappers.TowerBlockPlace;
import de.photon.aacadditionpro.util.VerboseSender;
import de.photon.aacadditionpro.util.entity.EntityUtil;
import de.photon.aacadditionpro.util.entity.PotionUtil;
import de.photon.aacadditionpro.util.exceptions.UnknownMinecraftVersion;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.inventory.InventoryUtils;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.world.BlockUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.potion.PotionEffectType;

public class Tower implements ListenerModule, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 120L);

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
            final Integer levitation;
            switch (ServerVersion.getActiveServerVersion()) {
                case MC188:
                    levitation = null;
                    break;
                case MC112:
                case MC113:
                case MC114:
                case MC115:
                case MC116:
                    levitation = PotionUtil.getAmplifier(PotionUtil.getPotionEffect(user.getPlayer(), PotionEffectType.LEVITATION));
                    break;
                default:
                    throw new UnknownMinecraftVersion();
            }

            // User must stand above the block (placed from above)1
            // Check if the block is tower-placed (Block belows)
            if (event.getBlock().getFace(event.getBlockAgainst()) == BlockFace.DOWN &&
                // The block is placed inside a 2 - block y-radius, this prevents false positives when building from a higher level
                user.getPlayer().getLocation().getY() - blockPlaced.getY() < 2D &&
                // Check if this check applies to the block
                blockPlaced.getType().isSolid() &&
                // Check if the block is placed against one block (face) only
                // Only one block that is not a liquid is allowed (the one which the Block is placed against).
                BlockUtils.getBlocksAround(blockPlaced, false).stream().filter(block -> !BlockUtils.LIQUIDS.contains(block.getType())).count() == 1 &&
                // User is not in water which can cause false positives due to faster swimming on newer versions.
                !EntityUtil.isHitboxInLiquids(user.getPlayer().getLocation(), user.getHitbox()) &&
                // Buffer the block place, continue the check only when we a certain number of block places in check
                user.getTowerData().getBlockPlaces().bufferObject(
                        new TowerBlockPlace(
                                blockPlaced,
                                //Jump boost effect is important
                                PotionUtil.getAmplifier(PotionUtil.getPotionEffect(user.getPlayer(), PotionEffectType.JUMP)),
                                levitation)))
            {
                // [0] = Expected time; [1] = Real time
                final double[] results = user.getTowerData().calculateTimes();

                // Real check
                if (results[1] < results[0]) {
                    final int vlToAdd = (int) Math.min(1 + Math.floor((results[0] - results[1]) / 16), 100);

                    // Violation-Level handling
                    vlManager.flag(event.getPlayer(), vlToAdd, cancelVl, () ->
                    {
                        event.setCancelled(true);
                        user.getTimestampMap().updateTimeStamp(TimestampKey.TOWER_TIMEOUT);
                        InventoryUtils.syncUpdateInventory(user.getPlayer());
                        // If not cancelled run the verbose message with additional data
                    }, () -> VerboseSender.getInstance().sendVerboseMessage("Tower-Verbose | Player: " + user.getPlayer().getName() + " expected time: " + results[0] + " | real: " + results[1]));
                }
            }
        }
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.TOWER;
    }
}
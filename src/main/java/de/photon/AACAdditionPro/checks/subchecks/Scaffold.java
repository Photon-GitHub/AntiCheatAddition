package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.user.data.PositionData;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.datawrappers.ScaffoldBlockPlace;
import de.photon.AACAdditionPro.util.entity.livingentity.PotionUtil;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.DoubleSummaryStatistics;

public class Scaffold implements Listener, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 140L);

    private final static double ANGLE_CHANGE_SUM_THRESHOLD = 11.3D;
    private final static double ANGLE_OFFSET_SUM_THRESHOLD = 10.5D;

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;

    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    @LoadFromConfiguration(configPath = ".parts.position.enabled")
    private boolean positionEnabled;
    @LoadFromConfiguration(configPath = ".parts.rotation.enabled")
    private boolean rotationEnabled;
    @LoadFromConfiguration(configPath = ".parts.safewalk.enabled")
    private boolean safeWalkEnabled;
    @LoadFromConfiguration(configPath = ".parts.sprinting.enabled")
    private boolean sprintingEnabled;

    @LoadFromConfiguration(configPath = ".parts.rotation.rotation_threshold")
    private int rotationThreshold;
    @LoadFromConfiguration(configPath = ".parts.sprinting.sprinting_threshold")
    private int sprintingThreshold;

    // ------------------------------------------- BlockPlace Handling ---------------------------------------------- //

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPre(final BlockPlaceEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user))
        {
            return;
        }

        // To prevent too fast scaffolding -> Timeout
        if (user.getScaffoldData().recentlyUpdated(0, timeout))
        {
            event.setCancelled(true);
            InventoryUtils.syncUpdateInventory(user.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(final BlockPlaceEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user))
        {
            return;
        }

        final Block blockPlaced = event.getBlockPlaced();

        // Short distance between player and the block (at most 2 Blocks)
        if (user.getPlayer().getLocation().distanceSquared(blockPlaced.getLocation()) < 4D &&
            // Not flying
            !user.getPlayer().isFlying() &&
            // Above the block
            user.getPlayer().getLocation().getY() > blockPlaced.getY() &&
            // Check if this check applies to the block
            blockPlaced.getType().isSolid() &&
            // Check if the block is placed against one block face only, also implies no blocks above and below.
            // Only one block that is not a liquid is allowed (the one which the Block is placed against).
            BlockUtils.getBlocksAround(blockPlaced, false).stream().filter(block -> !BlockUtils.LIQUIDS.contains(block.getType())).count() == 1 &&
            // In between check to make sure it is somewhat a scaffold movement as the buffering does not work.
            BlockUtils.HORIZONTAL_FACES.contains(event.getBlock().getFace(event.getBlockAgainst())) &&
            // Buffer the ScaffoldBlockPlace
            user.getScaffoldData().getScaffoldBlockPlaces().bufferObjectIgnoreSize(new ScaffoldBlockPlace(
                    blockPlaced,
                    blockPlaced.getFace(event.getBlockAgainst()),
                    // Speed-Effect
                    PotionUtil.getAmplifier(PotionUtil.getPotionEffect(user.getPlayer(), PotionEffectType.SPEED)),
                    user.getPlayer().getLocation().getYaw(),
                    user.getPositionData().hasPlayerMovedRecently(175, PositionData.MovementType.XZONLY)
            )))
        {
            final double xOffset = MathUtils.offset(user.getPlayer().getLocation().getX(), event.getBlockAgainst().getX());
            final double zOffset = MathUtils.offset(user.getPlayer().getLocation().getZ(), event.getBlockAgainst().getZ());

            int vl = 0;

            // --------------------------------------------- Positions ---------------------------------------------- //

            // Stopping part enabled
            if (this.positionEnabled)
            {
                boolean flag;
                switch (event.getBlock().getFace(event.getBlockAgainst()))
                {
                    case EAST:
                        flag = xOffset <= 0;
                        break;
                    case WEST:
                        flag = xOffset <= 1;
                        break;
                    case NORTH:
                        flag = zOffset <= 1;
                        break;
                    case SOUTH:
                        flag = zOffset <= 0;
                        break;
                    default:
                        // Some other, mostly weird blockplaces.
                        flag = false;
                        break;
                }

                if (flag)
                {
                    VerboseSender.sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " placed from a suspicious location.");
                    // Flag the player
                    vl += 4;
                }
            }


            // --------------------------------------------- Rotations ---------------------------------------------- //

            // Rotation part enabled
            if (this.rotationEnabled)
            {
                final DoubleSummaryStatistics angleChange = user.getLookPacketData().getAngleChange();
                final DoubleSummaryStatistics angleOffset = user.getLookPacketData().getOffsetAngleChange(angleChange.getAverage());

                byte rotationVl = 0;

                // Big rotation jumps in the last 2 ticks
                if (user.getLookPacketData().recentlyUpdated(0, 125))
                {
                    rotationVl += 3;
                    VerboseSender.sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 1");
                }

                // Generally high rotations
                if (angleChange.getSum() > ANGLE_CHANGE_SUM_THRESHOLD)
                {
                    rotationVl += 2;
                    VerboseSender.sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 2");
                }

                // Very random rotations
                if (angleOffset.getSum() > ANGLE_OFFSET_SUM_THRESHOLD)
                {
                    rotationVl += 1;
                    VerboseSender.sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 3");
                }

                if (rotationVl > 0)
                {
                    if (++user.getScaffoldData().rotationFails > this.rotationThreshold)
                    {
                        // Flag the player
                        vl += rotationVl;
                    }
                }
                else if (user.getScaffoldData().rotationFails > 0)
                {
                    user.getScaffoldData().rotationFails--;
                }
            }


            // --------------------------------------------- Sprinting ---------------------------------------------- //

            // Sprinting part enabled

            if (this.sprintingEnabled)
            {
                if (user.getPositionData().hasPlayerSprintedRecently(400))
                {
                    if (++user.getScaffoldData().sprintingFails > this.sprintingThreshold)
                    {
                        VerboseSender.sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sprinted suspiciously.");
                        // Flag the player
                        vl += 6;
                    }
                }
                else if (user.getScaffoldData().sprintingFails > 0)
                {
                    user.getScaffoldData().sprintingFails--;
                }
            }


            // ----------------------------------------- Suspicious stops ------------------------------------------- //

            // Stopping part enabled
            if (this.safeWalkEnabled)
            {
                byte flagVl = 0;

                // Moved to the edge of the block
                if (user.getPositionData().hasPlayerMovedRecently(175, PositionData.MovementType.XZONLY) &&
                    // Not sneaked recently. The sneaking must endure some time to prevent bypasses.
                    !(user.getPositionData().hasPlayerSneakedRecently(125) && user.getPositionData().getLastSneakTime() > 148))
                {
                    boolean sneakBorder;
                    switch (event.getBlock().getFace(event.getBlockAgainst()))
                    {
                        case EAST:
                            sneakBorder = xOffset > 0.28D && xOffset < 0.305D;
                            break;
                        case WEST:
                            sneakBorder = xOffset > 1.28D && xOffset < 1.305D;
                            break;
                        case NORTH:
                            sneakBorder = zOffset > 1.28D && zOffset < 1.305D;
                            break;
                        case SOUTH:
                            sneakBorder = zOffset > 0.28D && zOffset < 0.305D;
                            break;
                        default:
                            // Some other, mostly weird blockplaces.
                            sneakBorder = false;
                            break;
                    }

                    if (sneakBorder)
                    {
                        VerboseSender.sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " has behaviour associated with safe-walk. (Type 1)");
                        flagVl += 1;
                    }
                }

                // Moved recently
                if (user.getPositionData().hasPlayerMovedRecently(325, PositionData.MovementType.XZONLY) &&
                    // Suddenly stopped
                    !user.getPositionData().hasPlayerMovedRecently(125, PositionData.MovementType.XZONLY) &&
                    // Has not sneaked recently
                    !(user.getPositionData().hasPlayerSneakedRecently(175) && user.getPositionData().getLastSneakTime() > 148))
                {
                    VerboseSender.sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " has behaviour associated with safe-walk. (Type 2)");
                    flagVl += 2;
                }

                if (flagVl > 0)
                {
                    // Flag the player
                    vl += flagVl;
                }
            }


            // -------------------------------------------- Consistency --------------------------------------------- //

            // Should check average?
            if (user.getScaffoldData().getScaffoldBlockPlaces().hasReachedBufferSize())
            {
                // ------------------------------------- Consistency - Average -------------------------------------- //

                /*
                Indices:
                [0] -> Real average
                [1] -> Maximum allowed average
                 */
                final double[] results = user.getScaffoldData().calculateTimes();

                // delta-times are too low -> flag
                if (results[0] < results[1])
                {
                    // Flag the player
                    int vlIncrease = (int) (4 * Math.min(Math.ceil((results[1] - results[0]) / 15D), 6));

                    VerboseSender.sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " enforced delay: " + results[1] + " | real: " + results[0] + " | vl increase: " + vlIncrease);

                    vl += vlIncrease;
                }
            }

            if (vl > 0)
            {
                vlManager.flag(event.getPlayer(), vl, cancel_vl, () ->
                {
                    event.setCancelled(true);
                    user.getScaffoldData().updateTimeStamp(0);
                    InventoryUtils.syncUpdateInventory(user.getPlayer());
                }, () -> {});
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
        return ModuleType.SCAFFOLD;
    }
}
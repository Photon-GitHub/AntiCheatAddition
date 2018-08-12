package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.user.data.PositionData;
import de.photon.AACAdditionPro.user.datawrappers.ScaffoldBlockPlace;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.entity.PotionUtil;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import de.photon.AACAdditionPro.util.world.LocationUtils;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.potion.PotionEffectType;

public class Scaffold implements Listener, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 70L);

    private final static double ANGLE_CHANGE_SUM_THRESHOLD = 7D;
    private final static double ANGLE_OFFSET_SUM_THRESHOLD = 5.2D;

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;

    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    @LoadFromConfiguration(configPath = ".parts.position.enabled")
    private boolean positionEnabled;

    private boolean rotationCheckingNeeded = false;
    private final boolean[] rotationEnabled = new boolean[]{
            AACAdditionPro.getInstance().getConfig().getBoolean(this.getConfigString() + ".parts.rotation.type1"),
            AACAdditionPro.getInstance().getConfig().getBoolean(this.getConfigString() + ".parts.rotation.type2"),
            AACAdditionPro.getInstance().getConfig().getBoolean(this.getConfigString() + ".parts.rotation.type3")
    };

    private final boolean[] safeWalkEnabled = new boolean[]{
            AACAdditionPro.getInstance().getConfig().getBoolean(this.getConfigString() + ".parts.safewalk.type1"),
            AACAdditionPro.getInstance().getConfig().getBoolean(this.getConfigString() + ".parts.safewalk.type2")
    };

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
        if (User.isUserInvalid(user, this.getModuleType()))
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

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on(final BlockPlaceEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType()))
        {
            return;
        }

        final Block blockPlaced = event.getBlockPlaced();

        // Short distance between player and the block (at most 2 Blocks)
        if (LocationUtils.areLocationsInRange(user.getPlayer().getLocation(), blockPlaced.getLocation(), 4D) &&
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
                    user.getPositionData().hasPlayerSneakedRecently(175)
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
                    VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " placed from a suspicious location.");
                    // Flag the player
                    vl += 5;
                }
            }


            // --------------------------------------------- Rotations ---------------------------------------------- //

            // Rotation part enabled
            if (this.rotationCheckingNeeded)
            {
                final float[] angleInformation = user.getLookPacketData().getAngleInformation();

                byte rotationVl = 0;

                // Big rotation jumps in the last 2 ticks
                if (rotationEnabled[0] &&
                    user.getLookPacketData().recentlyUpdated(0, 125))
                {
                    rotationVl += 3;
                    VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 1");
                }

                // Generally high rotations
                if (rotationEnabled[1] &&
                    angleInformation[0] > ANGLE_CHANGE_SUM_THRESHOLD)
                {
                    rotationVl += 2;
                    VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 2");
                }

                // Very random rotations
                if (rotationEnabled[2] &&
                    angleInformation[1] > ANGLE_OFFSET_SUM_THRESHOLD)
                {
                    rotationVl += 1;
                    VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sent suspicious rotations. Type 3");
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
                        VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sprinted suspiciously.");
                        // Flag the player
                        vl += 8;
                    }
                }
                else if (user.getScaffoldData().sprintingFails > 0)
                {
                    user.getScaffoldData().sprintingFails--;
                }
            }


            // ----------------------------------------- Suspicious stops ------------------------------------------- //

            // Stopping part enabled
            if (this.safeWalkEnabled[0] &&
                // Moved to the edge of the block
                user.getPositionData().hasPlayerMovedRecently(175, PositionData.MovementType.XZONLY) &&
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
                    VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " has behaviour associated with safe-walk. (Type 1)");
                    vl += 1;
                }
            }

            if (safeWalkEnabled[1] &&
                // Moved recently
                user.getPositionData().hasPlayerMovedRecently(355, PositionData.MovementType.XZONLY) &&
                // Suddenly stopped
                !user.getPositionData().hasPlayerMovedRecently(175, PositionData.MovementType.XZONLY) &&
                // Has not sneaked recently
                !(user.getPositionData().hasPlayerSneakedRecently(175) && user.getPositionData().getLastSneakTime() > 148))
            {
                VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " has behaviour associated with safe-walk. (Type 2)");
                vl += 2;
            }


            // ---------------------------------------------- Average ----------------------------------------------- //

            // Should check average?
            if (user.getScaffoldData().getScaffoldBlockPlaces().hasReachedBufferSize())
            {
                /*
                Indices:
                [0] -> Expected time
                [1] -> Real time
                 */
                final double[] results = user.getScaffoldData().calculateTimes();

                // delta-times are too low -> flag
                if (results[1] < results[0])
                {
                    // Flag the player
                    final int vlIncrease = (int) (4 * Math.min(Math.ceil((results[0] - results[1]) / 15D), 6));

                    VerboseSender.getInstance().sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " enforced delay: " + results[0] + " | real: " + results[1] + " | vl increase: " + vlIncrease);

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
    public void subEnable()
    {
        for (boolean b : this.rotationEnabled)
        {
            if (b)
            {
                this.rotationCheckingNeeded = true;
                return;
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
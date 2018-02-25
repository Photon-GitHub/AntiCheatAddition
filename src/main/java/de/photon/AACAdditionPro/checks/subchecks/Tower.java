package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.datawrappers.TowerBlockPlace;
import de.photon.AACAdditionPro.util.entity.livingentity.PotionUtil;
import de.photon.AACAdditionPro.util.fakeentity.movement.Gravitation;
import de.photon.AACAdditionPro.util.fakeentity.movement.Jumping;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.DoubleSummaryStatistics;

public class Tower implements Listener, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 120L);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;

    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    @LoadFromConfiguration(configPath = ".tower_leniency")
    private double tower_leniency;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPre(final BlockPlaceEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user))
        {
            return;
        }

        // To prevent too fast towering -> Timeout
        if (user.getTowerData().recentlyUpdated(0, timeout))
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

        // Not flying
        if (!user.getPlayer().isFlying())
        {
            final Block blockPlaced = event.getBlockPlaced();
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
                // Buffer the block place, continue the check only when we a certain number of block places in check
                user.getTowerData().bufferBlockPlace(
                        new TowerBlockPlace(
                                blockPlaced,
                                //Jump boost effect is important
                                PotionUtil.getAmplifier(PotionUtil.getPotionEffect(user.getPlayer(), PotionEffectType.JUMP))
                        )))
            {
                final DoubleSummaryStatistics summaryStatistics = new DoubleSummaryStatistics();

                for (final TowerBlockPlace blockPlace : user.getTowerData().getBlockPlaces())
                {
                    summaryStatistics.accept(calculateDelay(blockPlace.getJumpBoostLevel()));
                }

                // Apply lenience
                final double lenientThreshold = summaryStatistics.getAverage();

                // Real average
                final double average = user.getTowerData().calculateAverageTime();

                // Real check
                if (average < lenientThreshold)
                {
                    final int vlToAdd = (int) Math.min(1 + Math.floor((lenientThreshold - average) / 16), 100);

                    // Violation-Level handling
                    vlManager.flag(event.getPlayer(), vlToAdd, cancel_vl, () ->
                    {
                        event.setCancelled(true);
                        user.getTowerData().updateTimeStamp(0);
                        InventoryUtils.syncUpdateInventory(user.getPlayer());
                        // If not cancelled run the verbose message with additional data
                    }, () -> VerboseSender.sendVerboseMessage("Tower-Verbose | Player: " + user.getPlayer().getName() + " expected time: " + lenientThreshold + " | real: " + average));
                }
            }
        }
    }


    /**
     * Calculates the time needed to place one block.
     *
     * @param amplifier the JUMP_BOOST amplifier the person had while placing the block, or null if he did not have the JUMP_BOOST effect
     */
    private double calculateDelay(final Integer amplifier)
    {
        // No JUMP_BOOST
        if (amplifier == null)
        {
            // 478.4 * 0.925
            return 442.52;
        }

        // Player has JUMP_BOOST
        if (amplifier < 0)
        {
            // Negative JUMP_BOOST -> Not allowed to place blocks -> Very high delay
            return 1500;
        }

        // How many blocks can potentially be placed during one jump cycle
        short maximumPlacedBlocks = 1;

        // The velocity in the beginning
        Vector currentVelocity = new Vector(0, Jumping.getJumpYMotion(amplifier), 0);

        // The first tick (1) happens here
        double currentBlockValue = currentVelocity.getY();

        // Start the tick-loop at 2 due to the one tick outside.
        for (short ticks = 2; ticks < 160; ticks++)
        {
            currentVelocity = Gravitation.applyGravitationAndAirResistance(currentVelocity, Gravitation.PLAYER);

            currentBlockValue += currentVelocity.getY();

            // The maximum placed blocks are the next lower integer of the maximum y-Position of the player
            final short flooredBlocks = (short) Math.floor(currentBlockValue);
            if (maximumPlacedBlocks < flooredBlocks)
            {
                maximumPlacedBlocks = flooredBlocks;
            }
            else
            {
                // Location must be lower than maximumPlacedBlocks and there is negative velocity (in the beginning there is no negative velocity, but maximumPlacedBlocks > flooredBlocks!)
                if (maximumPlacedBlocks > flooredBlocks && currentVelocity.getY() < 0)
                {
                    // Leniency:
                    double leniency;
                    switch (amplifier)
                    {
                        case 0:
                            leniency = 1;
                            break;
                        case 1:
                        case 3:
                            leniency = 0.9;
                            break;
                        case 2:
                            leniency = 0.87;
                            break;
                        default:
                            leniency = 0.982;
                            break;
                    }

                    // If the result is lower here, the detection is more lenient.
                    // Convert ticks to milliseconds
                    return ((ticks * 50) / maximumPlacedBlocks) * leniency * tower_leniency;
                }
            }
        }

        // Too high movement; no checking
        return 0;
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
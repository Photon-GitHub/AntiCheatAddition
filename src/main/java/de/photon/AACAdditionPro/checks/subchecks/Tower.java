package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.entities.movement.Gravitation;
import de.photon.AACAdditionPro.util.entities.movement.Jumping;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import de.photon.AACAdditionPro.util.storage.datawrappers.BlockPlace;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class Tower implements Listener, AACAdditionProCheck
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 120L);

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
        if (AACAdditionProCheck.isUserInvalid(user)) {
            return;
        }

        //To prevent too fast towering -> Timeout
        if (user.getTowerData().recentlyUpdated(timeout)) {
            event.setCancelled(true);
            InventoryUtils.syncUpdateInventory(user.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(final BlockPlaceEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (AACAdditionProCheck.isUserInvalid(user)) {
            return;
        }

        // Not flying
        if (!user.getPlayer().isFlying()) {
            final Block blockPlaced = event.getBlockPlaced();
            // User must stand above the block (placed from above)1
            // Check if the block is tower-placed (Block belows)
            if (event.getBlockAgainst().equals(blockPlaced.getRelative(BlockFace.DOWN)) &&
                // The block is placed inside a 2 - block y-radius, this prevents false positives when building from a higher level
                user.getPlayer().getLocation().getY() - blockPlaced.getY() < 2D &&
                // Check if this check applies to the block
                blockPlaced.getType().isSolid() &&
                // Check if the block is placed against one block (face) only
                BlockUtils.blocksAround(blockPlaced, false) == (byte) 1 &&
                // Buffer the block place, continue the check only when we a certain number of block places in check
                user.getTowerData().bufferBlockPlace(
                        new BlockPlace(
                                System.currentTimeMillis(), blockPlaced,
                                //Speed is not important for this check
                                null,
                                //Jump boost effect is important
                                user.getPotionData().getAmplifier(PotionEffectType.JUMP)
                        )))
            {
                // The buffer is filled to the required degree -> Checking now
                double threshold = 0;

                for (final BlockPlace blockPlace : user.getTowerData().getBlockPlaces()) {
                    threshold += calculateDelay(blockPlace.getJumpBoostLevel());
                }

                // Expected Average
                threshold /= user.getTowerData().getBuffer_size();

                // Apply lenience
                final double lenientThreshold = threshold * tower_leniency;

                // Real average
                final double average = user.getTowerData().calculateRealTime();
                // System.out.println("Average: " + average);

                // Real check
                if (average < threshold) {
                    final int vlToAdd = (int) Math.min(1 + Math.floor((threshold - average) / 16), 100);

                    // Violation-Level handling
                    vlManager.flag(event.getPlayer(), vlToAdd, cancel_vl, () ->
                    {
                        event.setCancelled(true);
                        user.getTowerData().updateTimeStamp();
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
        if (amplifier == null) {
            return 478.4;
        }

        // Player has JUMP_BOOST
        if (amplifier <= 0) {
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
        for (short ticks = 2; ticks < 160; ticks++) {
            currentVelocity = Gravitation.applyGravitationAndAirResistance(currentVelocity, Gravitation.PLAYER);

            currentBlockValue += currentVelocity.getY();

            // The maximum placed blocks are the next lower integer of the maximum y-Position of the player
            final short flooredBlocks = (short) Math.floor(currentBlockValue);
            // System.out.println("Real-Blocks: " + currentBlockValue + "Floored-Blocks: " + flooredBlocks);
            if (maximumPlacedBlocks < flooredBlocks) {
                maximumPlacedBlocks = flooredBlocks;
            } else {
                // Location must be lower than maximumPlacedBlocks and there is negative velocity (in the beginning there is no negative velocity, but maximumPlacedBlocks > flooredBlocks!)
                if (maximumPlacedBlocks > flooredBlocks && currentVelocity.getY() < 0) {
                    // Convert ticks to milliseconds
                    // System.out.println("Max-Blocks: " + maximumPlacedBlocks);
                    // System.out.println("TowerReal: " + (ticks * 50) / maximumPlacedBlocks);
                    return (ticks * 50) / maximumPlacedBlocks;
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
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.TOWER;
    }
}
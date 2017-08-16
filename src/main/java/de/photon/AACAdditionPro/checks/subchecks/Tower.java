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

    @LoadFromConfiguration(configPath = ".jump_boost_leniency")
    private double jump_boost_leniency;

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
                                Short.MIN_VALUE,
                                //Jump boost effect is important
                                user.getPotionData().getAmplifier(PotionEffectType.JUMP)
                        )))
            {
                // The buffer is filled to the required degree -> Checking now
                double threshold = 0;

                // Iterate through the buffer
                for (final BlockPlace blockPlace : user.getTowerData().getBlockPlaces()) {
                    final double addThreshold = calculateDelay(blockPlace.getJumpBoostLevel());
                    threshold += addThreshold;
                }

                // Expected Average
                threshold /= user.getTowerData().getBuffer_size();

                // Apply lenience
                final double lenientThreshold = threshold * tower_leniency;

                // Real average
                final double average = user.getTowerData().calculateRealTime();
                System.out.println("Average: " + average);

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
     * @param amplifier the JUMP_BOOST amplifier the person had while placing the block,
     *                  incremented by one (Speed I -> 1) or -1 if the effect is not present
     */
    private double calculateDelay(final short amplifier)
    {
        switch (amplifier) {
            // No potion-effects at all
            //case Short.MIN_VALUE:
            //    return 478.5;
            default:
                if (amplifier <= 0) {
                    // Not allowed to place blocks -> Very high delay
                    return 1500;
                }

                // How many blocks can potentially be placed during one jump cycle
                short maximumPlacedBlocks = 1;

                final double tick_step = 0.25;

                // The velocity in the beginning
                Vector currentVelocity = new Vector(0, Jumping.getJumpYMotion(amplifier), 0);

                // The first tick is ignored in the loop
                double currentBlockValue = 0;

                for (double ticks = 0D; ticks < 160D; ticks += tick_step) {
                    currentVelocity = Gravitation.applyGravitationAndAirResistance(currentVelocity, Gravitation.PLAYER, tick_step);

                    currentBlockValue += (currentVelocity.getY() * tick_step);

                    // The maximum placed blocks are the next lower integer of the maximum y-Position of the player
                    final short flooredBlocks = (short) Math.floor(currentBlockValue);
                    if (maximumPlacedBlocks < flooredBlocks) {
                        maximumPlacedBlocks = flooredBlocks;
                    } else {
                        // Location must be lower than maximumPlacedBlocks (negative velocity is automatically granted when breaking the loop)
                        if (maximumPlacedBlocks > flooredBlocks) {
                            // Convert ticks to milliseconds
                            System.out.println("TowerReal: " + ticks * 50);
                            return (ticks * 50 * (1 + jump_boost_leniency)) / maximumPlacedBlocks;
                        }
                    }
                }

                // Too high movement; no checking
                return 0;
        }
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
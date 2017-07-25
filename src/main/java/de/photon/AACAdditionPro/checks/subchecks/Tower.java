package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.entities.movement.Jumping;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.storage.datawrappers.BlockPlace;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.potion.PotionEffectType;

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
            updateInventoryNextTick(user);
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
                threshold *= tower_leniency;

                // Real average
                final double average = user.getTowerData().calculateRealTime();

                // Real check
                if (average < threshold) {
                    final int vlToAdd = (int) Math.min(1 + Math.floor((threshold - average) / 16), 100);

                    // Violation-Level handling
                    final double finalThreshold = threshold;
                    vlManager.flag(event.getPlayer(), vlToAdd, cancel_vl, () ->
                    {
                        event.setCancelled(true);
                        user.getTowerData().updateTimeStamp();
                        updateInventoryNextTick(user);
                        // If not cancelled run the verbose message with additional data
                    }, () -> VerboseSender.sendVerboseMessage("Tower-Verbose | Player: " + user.getPlayer().getName() + " expected time: " + finalThreshold + " | real: " + average));
                }
            }
        }
    }

    // The amplifier will be incremented by one (Speed I -> 1) or -1 if the effect is not present
    private double calculateDelay(final short amplifier)
    {
        switch (amplifier) {
            // No potion-effects at all
            case Short.MIN_VALUE:
                return 478.5;
            default:
                if (amplifier <= 0) {
                    // Not allowed to place blocks -> High delay
                    return 1000;
                }

                // How many blocks can potentially be placed
                short maximumPlacedBlocks = 1;

                final double tick_step = 0.25;

                // The velocity in the beginning
                final double starting_velocity = Jumping.getJumpYMotion(amplifier);

                // The acceleration of the gravitation in
                // blocks / tick
                final double gravitational_acceleration = -0.08D;

                // This represents the drag in the form of
                // (1 - drag) / tick
                final double formula_drag = 0.98D;

                // The first tick is ignored in the loop
                double lastBlockValue = 0;
                double currentBlockValue = starting_velocity;

                double currentYMotion = starting_velocity;

                for (double ticks = 0; ticks < 160; ticks += tick_step) {
                    // 0.25 * 0.08
                    currentYMotion += gravitational_acceleration * tick_step;
                    // x^4 = 0.98
                    currentYMotion *= Math.pow(formula_drag, tick_step);

                    // Due to the quarter tick
                    currentBlockValue += (currentYMotion * tick_step);

                    // The maximum placed blocks are the next lower integer of the maximum y-Position of the player
                    final short flooredBlocks = (short) Math.floor(currentBlockValue);
                    if (maximumPlacedBlocks < flooredBlocks) {
                        maximumPlacedBlocks = flooredBlocks;
                    }

                    // Second solution
                    if (lastBlockValue > maximumPlacedBlocks && currentBlockValue <= maximumPlacedBlocks) {
                        // Apply the leniency here, as a block-wise implementation is needed.
                        return (ticks * 50 * jump_boost_leniency) / maximumPlacedBlocks;
                    }
                    lastBlockValue = currentBlockValue;
                }

                // Too high movement; no checking
                return 0;
        }
    }

    private static void updateInventoryNextTick(final User user)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(AACAdditionPro.getInstance(), () -> user.getPlayer().updateInventory(), 1L);
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
package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.checks.AACAdditionProCheck;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
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

public class Scaffold implements Listener, AACAdditionProCheck
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getAdditionHackType(), 100L);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;
    @LoadFromConfiguration(configPath = ".scaffold_delay")
    private int scaffold_delay;
    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPre(final BlockPlaceEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null || user.isBypassed()) {
            return;
        }

        //To prevent too fast towering -> Timeout
        if (user.getScaffoldData().recentlyUpdated(timeout)) {
            event.setCancelled(true);
            Bukkit.getScheduler().scheduleSyncDelayedTask(
                    AACAdditionPro.getInstance(),
                    () -> user.getPlayer().updateInventory(), 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void on(final BlockPlaceEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (user == null || user.isBypassed()) {
            return;
        }

        //Failed the check
        if (check(user, event)) {
            vlManager.flag(event.getPlayer(), cancel_vl, () ->
            {
                event.setCancelled(true);
                user.getScaffoldData().updateTimeStamp();
                Bukkit.getScheduler().scheduleSyncDelayedTask(
                        AACAdditionPro.getInstance(),
                        () -> user.getPlayer().updateInventory(), 1L);
            }, () -> {});
        }
    }

    private boolean check(final User user, final BlockPlaceEvent event)
    {
        final Block blockPlaced = event.getBlockPlaced();

        // Short distance between player and the block (2 Blocks)
        if (user.getPlayer().getLocation().distanceSquared(blockPlaced.getLocation()) < 4D &&
            // Not flying
            !user.getPlayer().isFlying() &&
            user.getPlayer().getLocation().getY() > blockPlaced.getY() - 1 &&
            // Check if this check applies to the block
            blockPlaced.getType().isSolid() &&
            // Check if the block is placed in the air
            blockPlaced.getRelative(BlockFace.UP).isEmpty() && blockPlaced.getRelative(BlockFace.DOWN).isEmpty() &&
            // Check if the block is placed against one block face only
            BlockUtils.blocksAround(blockPlaced, true) == (byte) 1 &&
            // Buffer the block place, continue the check only when we a certain number of block places in check
            user.getScaffoldData().bufferBlockPlace(
                    new BlockPlace(
                            System.currentTimeMillis(), blockPlaced,
                            //Speed-Effect
                            user.getPotionData().getAmplifier(PotionEffectType.SPEED),
                            //JumpBoost effect is 0 because it is not relevant for the check
                            (byte) 0
                    )))
        {
            // If the buffer is big enough calculate an average time
            final double average = user.getScaffoldData().calculateRealTime();

            if (average < scaffold_delay) {
                VerboseSender.sendVerboseMessage("SCAFFOLD | Player: " + user.getPlayer().getName() + " estimated time: " + scaffold_delay + " | real: " + average);
                return true;
            }
        }
        return false;
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public AdditionHackType getAdditionHackType()
    {
        return AdditionHackType.SCAFFOLD;
    }

}
package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
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

public class Scaffold implements Listener, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 100L);

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
        if (User.isUserInvalid(user))
        {
            return;
        }

        //To prevent too fast towering -> Timeout
        if (user.getScaffoldData().recentlyUpdated(timeout))
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
                            blockPlaced,
                            // Speed-Effect
                            user.getPotionData().getAmplifier(PotionEffectType.SPEED),
                            // JumpBoost effect is 0 because it is not relevant for the check
                            null
                    )))
        {
            // If the buffer is big enough calculate an average time
            final double average = user.getScaffoldData().calculateRealTime();

            // delta-times are too low -> flag
            if (average < scaffold_delay)
            {
                VerboseSender.sendVerboseMessage("SCAFFOLD | Player: " + user.getPlayer().getName() + " estimated time: " + scaffold_delay + " | real: " + average);

                // Flag the player
                vlManager.flag(event.getPlayer(), (int) Math.max(Math.min(1, (scaffold_delay - average) / 15), 6), cancel_vl, () ->
                {
                    event.setCancelled(true);
                    user.getScaffoldData().updateTimeStamp();
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
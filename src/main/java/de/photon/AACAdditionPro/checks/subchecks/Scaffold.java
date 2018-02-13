package de.photon.AACAdditionPro.checks.subchecks;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.VerboseSender;
import de.photon.AACAdditionPro.util.datawrappers.ScaffoldBlockPlace;
import de.photon.AACAdditionPro.util.entity.livingentity.PotionUtil;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.potion.PotionEffectType;

public class Scaffold extends PacketAdapter implements Listener, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 100L);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;

    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    @LoadFromConfiguration(configPath = ".rotation_threshold")
    private int rotationThreshold;

    public Scaffold()
    {
        super(AACAdditionPro.getInstance(), ListenerPriority.LOW, PacketType.Play.Client.POSITION_LOOK, PacketType.Play.Client.LOOK);
    }

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

        //To prevent too fast towering -> Timeout
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
            user.getPlayer().getLocation().getY() > blockPlaced.getY() - 1 &&
            // Check if this check applies to the block
            blockPlaced.getType().isSolid() &&
            // Check if the block is placed against one block face only, also implies no blocks above and below.
            BlockUtils.blocksAround(blockPlaced, false) == (byte) 1)
        {
            

            // ------------------------------------------ Fast Rotations -------------------------------------------- //

            // Rotation check enabled
            if (this.rotationThreshold >= 0)
            {
                if (user.getLookPacketData().recentlyUpdated(1, 100))
                {
                    if (++user.getScaffoldData().rotationFails > this.rotationThreshold)
                    {
                        VerboseSender.sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " sent suspicious rotations.");
                        // Flag the player
                        vlManager.flag(event.getPlayer(), 1, cancel_vl, () ->
                        {
                            event.setCancelled(true);
                            user.getScaffoldData().updateTimeStamp(0);
                            InventoryUtils.syncUpdateInventory(user.getPlayer());
                        }, () -> {});
                    }
                }
                else if (user.getScaffoldData().rotationFails > 0)
                {
                    user.getScaffoldData().rotationFails--;
                }
            }


            // -------------------------------------------- Consistency --------------------------------------------- //
            // Buffer the block place, continue the check only when we a certain number of block places in check
            {
                if (user.getScaffoldData().bufferBlockPlace(
                        new ScaffoldBlockPlace(
                                blockPlaced,
                                blockPlaced.getFace(event.getBlockAgainst()),
                                // Speed-Effect
                                PotionUtil.getAmplifier(PotionUtil.getPotionEffect(user.getPlayer(), PotionEffectType.SPEED))
                        )))
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
                        VerboseSender.sendVerboseMessage("Scaffold-Verbose | Player: " + user.getPlayer().getName() + " enforced delay: " + results[1] + " | real: " + results[0]);

                        // Flag the player
                        vlManager.flag(event.getPlayer(), (int) Math.max(Math.ceil((results[1] - results[0]) / 15D), 6), cancel_vl, () ->
                        {
                            event.setCancelled(true);
                            user.getScaffoldData().updateTimeStamp(0);
                            InventoryUtils.syncUpdateInventory(user.getPlayer());
                        }, () -> {});
                    }
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
        return ModuleType.SCAFFOLD;
    }
}
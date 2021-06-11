package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.inventory.InventoryUtil;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import de.photon.aacadditionpro.util.world.BlockUtil;
import de.photon.aacadditionpro.util.world.LocationUtil;
import de.photon.aacadditionpro.util.world.MaterialUtil;
import de.photon.aacadditionproold.user.subdata.datawrappers.ScaffoldBlockPlace;
import de.photon.aacadditionproold.util.inventory.InventoryUtils;
import de.photon.aacadditionproold.util.potion.InternalPotionEffectType;
import de.photon.aacadditionproold.util.potion.PotionUtil;
import lombok.Getter;
import lombok.val;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class Scaffold extends ViolationModule implements Listener
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 80L);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private int cancelVl;

    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    @LoadFromConfiguration(configPath = ".parts.rotation.violation_threshold")
    private int rotationThreshold;

    public Scaffold(String configString)
    {
        super(configString);
    }

    // ------------------------------------------- BlockPlace Handling ---------------------------------------------- //

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPreBlockPlace(final BlockPlaceEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        // To prevent too fast scaffolding -> Timeout
        if (user.getTimestampMap().at(TimestampKey.SCAFFOLD_TIMEOUT).recentlyUpdated(timeout)) {
            event.setCancelled(true);
            InventoryUtil.syncUpdateInventory(user.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event)
    {
        val user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        val blockPlaced = event.getBlockPlaced();

        // Short distance between player and the block (at most 2 Blocks)
        if (LocationUtil.areLocationsInRange(user.getPlayer().getLocation(), blockPlaced.getLocation(), 4D) &&
            // Not flying
            !user.getPlayer().isFlying() &&
            // Above the block
            user.getPlayer().getLocation().getY() > blockPlaced.getY() &&
            // Check if this check applies to the block
            blockPlaced.getType().isSolid() &&
            // Ladders and Vines are prone to false positives as they can be used to place blocks immediately after placing
            // them, therefore almost doubling the placement speed. However they can only be placed one at a time, which
            // allows simply ignoring them.
            event.getBlockPlaced().getType() != Material.LADDER && event.getBlockPlaced().getType() != Material.VINE &&
            // Check if the block is placed against one block face only, also implies no blocks above and below.
            // Only one block that is not a liquid is allowed (the one which the Block is placed against).
            BlockUtil.countBlocksAround(blockPlaced, BlockUtil.ALL_FACES, MaterialUtil.LIQUIDS) == 1 &&
            // In between check to make sure it is somewhat a scaffold movement as the buffering does not work.
            BlockUtil.HORIZONTAL_FACES.contains(event.getBlock().getFace(event.getBlockAgainst())))
        {

            val lastScaffoldBlock = user.getScaffoldData().getScaffoldBlockPlaces().peekLastAdded().getBlock();
            // This checks if the block was placed against the expected block for scaffolding.
            val newSituation = !lastScaffoldBlock.equals(event.getBlockAgainst()) || !BlockUtil.isNext(lastScaffoldBlock, event.getBlockPlaced(), true);

            // ---------------------------------------------- Average ---------------------------------------------- //

            if (newSituation) {
                user.getScaffoldData().getScaffoldBlockPlaces().clear();
            }

            user.getScaffoldData().getScaffoldBlockPlaces().addDataPoint(new ScaffoldBlockPlace(
                    event.getBlockPlaced(),
                    event.getBlockPlaced().getFace(event.getBlockAgainst()),
                    // Speed-Effect
                    PotionUtil.getAmplifier(PotionUtil.getPotionEffect(user.getPlayer(), InternalPotionEffectType.SPEED)),
                    user.getPlayer().getLocation().getYaw(),
                    user.hasSneakedRecently(175)));

            // --------------------------------------------- Rotations ---------------------------------------------- //

            int vl = AnglePattern.getInstance().getApplyingConsumer().applyAsInt(user, event);
            vl += PositionPattern.getInstance().getApplyingConsumer().applyAsInt(user, event);

            // All these checks may have false positives in new situations.
            if (!newSituation) {
                final float[] angleInformation = user.getLookPacketData().getAngleInformation();

                int rotationVl = RotationTypeOnePattern.getInstance().getApplyingConsumer().applyAsInt(user) +
                                 RotationTypeTwoPattern.getInstance().getApplyingConsumer().applyAsInt(user, angleInformation[0]) +
                                 RotationTypeThreePattern.getInstance().getApplyingConsumer().applyAsInt(user, angleInformation[1]);

                if (rotationVl > 0) {
                    if (++user.getScaffoldData().rotationFails >= this.rotationThreshold) {
                        // Flag the player
                        vl += rotationVl;
                    }
                } else if (user.getScaffoldData().rotationFails > 0) {
                    --user.getScaffoldData().rotationFails;
                }

                vl += SafewalkTypeOnePattern.getInstance().getApplyingConsumer().applyAsInt(user, event);
                vl += SafewalkTypeTwoPattern.getInstance().getApplyingConsumer().applyAsInt(user);
                vl += SprintingPattern.getInstance().getApplyingConsumer().applyAsInt(user);
            }

            if (vl > 0) {
                vlManager.flag(event.getPlayer(), vl, cancelVl, () ->
                {
                    event.setCancelled(true);
                    user.getTimestampMap().updateTimeStamp(TimestampKey.SCAFFOLD_TIMEOUT);
                    InventoryUtils.syncUpdateInventory(user.getPlayer());
                }, () -> {});
            }
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).withDecay(80, 1).build();
    }
}
package de.photon.aacadditionpro.modules.checks.scaffold;

import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.modules.BatchProcessorModule;
import de.photon.aacadditionpro.modules.ListenerModule;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.user.subdata.datawrappers.ScaffoldBlockPlace;
import de.photon.aacadditionpro.util.datastructures.batch.BatchProcessor;
import de.photon.aacadditionpro.util.files.configs.LoadFromConfiguration;
import de.photon.aacadditionpro.util.inventory.InventoryUtils;
import de.photon.aacadditionpro.util.potion.InternalPotionEffectType;
import de.photon.aacadditionpro.util.potion.PotionUtil;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.world.BlockUtils;
import de.photon.aacadditionpro.util.world.LocationUtils;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Set;

public class Scaffold implements BatchProcessorModule<ScaffoldBlockPlace>, ListenerModule, ViolationModule
{
    @Getter
    private static final Scaffold instance = new Scaffold();

    private static final Set<Module> submodules = ImmutableSet.of(AnglePattern.getInstance(),
                                                                  PositionPattern.getInstance(),
                                                                  RotationTypeOnePattern.getInstance(),
                                                                  RotationTypeTwoPattern.getInstance(),
                                                                  RotationTypeThreePattern.getInstance(),
                                                                  SafewalkTypeOnePattern.getInstance(),
                                                                  SafewalkTypeTwoPattern.getInstance(),
                                                                  SprintingPattern.getInstance());

    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 80L);

    @LoadFromConfiguration(configPath = ".cancel_vl")
    @Getter
    private int cancelVl;

    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    @LoadFromConfiguration(configPath = ".parts.rotation.violation_threshold")
    private int rotationThreshold;

    // ------------------------------------------- BlockPlace Handling ---------------------------------------------- //

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPreBlockPlace(final BlockPlaceEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
            return;
        }

        // To prevent too fast scaffolding -> Timeout
        if (user.getTimestampMap().recentlyUpdated(TimestampKey.SCAFFOLD_TIMEOUT, timeout)) {
            event.setCancelled(true);
            InventoryUtils.syncUpdateInventory(user.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event)
    {
        final User user = UserManager.getUser(event.getPlayer().getUniqueId());

        // Not bypassed
        if (User.isUserInvalid(user, this.getModuleType())) {
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
            // Ladders and Vines are prone to false positives as they can be used to place blocks immediately after placing
            // them, therefore almost doubling the placement speed. However they can only be placed one at a time, which
            // allows simply ignoring them.
            event.getBlockPlaced().getType() != Material.LADDER && event.getBlockPlaced().getType() != Material.VINE &&
            // Check if the block is placed against one block face only, also implies no blocks above and below.
            // Only one block that is not a liquid is allowed (the one which the Block is placed against).
            BlockUtils.countBlocksAround(blockPlaced, true) == 1 &&
            // In between check to make sure it is somewhat a scaffold movement as the buffering does not work.
            BlockUtils.HORIZONTAL_FACES.contains(event.getBlock().getFace(event.getBlockAgainst())))
        {

            final Block lastScaffoldBlock = user.getScaffoldData().getScaffoldBlockPlaces().peekLastAdded().getBlock();
            // This checks if the block was placed against the expected block for scaffolding.
            final boolean newSituation = !lastScaffoldBlock.equals(event.getBlockAgainst()) || !BlockUtils.isNext(lastScaffoldBlock, event.getBlockPlaced(), true);

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
                    user.getScaffoldData().rotationFails--;
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
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public Set<Module> getSubModules()
    {
        return submodules;
    }

    @Override
    public boolean isSubModule()
    {
        return false;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.SCAFFOLD;
    }

    @Override
    public BatchProcessor<ScaffoldBlockPlace> getBatchProcessor()
    {
        return AverageBatchProcessor.getInstance();
    }
}
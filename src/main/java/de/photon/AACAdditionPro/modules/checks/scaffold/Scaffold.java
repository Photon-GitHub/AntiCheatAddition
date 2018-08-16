package de.photon.AACAdditionPro.modules.checks.scaffold;

import com.google.common.collect.ImmutableSet;
import de.photon.AACAdditionPro.modules.ListenerModule;
import de.photon.AACAdditionPro.modules.ModuleType;
import de.photon.AACAdditionPro.modules.PatternModule;
import de.photon.AACAdditionPro.modules.ViolationModule;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.user.datawrappers.ScaffoldBlockPlace;
import de.photon.AACAdditionPro.util.entity.PotionUtil;
import de.photon.AACAdditionPro.util.files.configs.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.inventory.InventoryUtils;
import de.photon.AACAdditionPro.util.violationlevels.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.world.BlockUtils;
import de.photon.AACAdditionPro.util.world.LocationUtils;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

public class Scaffold implements ListenerModule, PatternModule, ViolationModule
{
    private final ViolationLevelManagement vlManager = new ViolationLevelManagement(this.getModuleType(), 70L);

    private final Pattern<User, BlockPlaceEvent> anglePattern = new AnglePattern();
    private final Pattern<User, BlockPlaceEvent> averagePattern = new AveragePattern();
    private final Pattern<User, BlockPlaceEvent> positionPattern = new PositionPattern();
    private final Pattern<User, BlockPlaceEvent> rotationTypeOne = new RotationTypeOnePattern();
    private final Pattern<User, Float> rotationTypeTwo = new RotationTypeTwoPattern();
    private final Pattern<User, Float> rotationTypeThree = new RotationTypeThreePattern();
    private final Pattern<User, BlockPlaceEvent> sprintingPattern = new SprintingPattern();
    private final Pattern<User, BlockPlaceEvent> safewalkTypeOne = new SafewalkTypeOnePattern();
    private final Pattern<User, BlockPlaceEvent> safewalkTypeTwo = new SafewalkTypeTwoPattern();

    @LoadFromConfiguration(configPath = ".cancel_vl")
    private int cancel_vl;

    @LoadFromConfiguration(configPath = ".timeout")
    private int timeout;

    @LoadFromConfiguration(configPath = ".parts.rotation.rotation_threshold")
    private int rotationThreshold;

    // ------------------------------------------- BlockPlace Handling ---------------------------------------------- //

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPreBlockPlace(final BlockPlaceEvent event)
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
    public void onBlockPlace(final BlockPlaceEvent event)
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
            int vl = anglePattern.apply(user, event);
            vl += averagePattern.apply(user, event);
            vl += positionPattern.apply(user, event);

            // --------------------------------------------- Rotations ---------------------------------------------- //

            final float[] angleInformation = user.getLookPacketData().getAngleInformation();

            int rotationVl = rotationTypeOne.apply(user, event) +
                             rotationTypeTwo.apply(user, angleInformation[0]) +
                             rotationTypeThree.apply(user, angleInformation[1]);

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

            vl += sprintingPattern.apply(user, event);
            vl += safewalkTypeOne.apply(user, event);
            vl += safewalkTypeTwo.apply(user, event);

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
    public Set<Pattern> getPatterns()
    {
        return ImmutableSet.of(anglePattern,
                               averagePattern,
                               positionPattern,
                               rotationTypeOne,
                               rotationTypeTwo,
                               rotationTypeThree,
                               sprintingPattern,
                               safewalkTypeOne,
                               safewalkTypeTwo);
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
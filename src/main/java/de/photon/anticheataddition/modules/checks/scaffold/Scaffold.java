package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.user.data.batch.ScaffoldBatch;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.messaging.Log;
import de.photon.anticheataddition.util.minecraft.world.WorldUtil;
import de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.Objects;

@Getter
public final class Scaffold extends ViolationModule implements Listener
{
    public static final Scaffold INSTANCE = new Scaffold();

    private final int cancelVl = loadInt(".cancel_vl", 110);
    private final int timeout = loadInt(".timeout", 1000);
    private final int placementDelay = loadInt(".placement_delay", 238);

    private Scaffold()
    {
        super("Scaffold", ScaffoldAngle.INSTANCE,
              ScaffoldFace.INSTANCE,
              ScaffoldJumping.INSTANCE,
              ScaffoldPosition.INSTANCE,
              ScaffoldRotation.INSTANCE,
              ScaffoldSafewalkEdge.INSTANCE,
              ScaffoldSafewalkTiming.INSTANCE,
              ScaffoldSprinting.INSTANCE);
    }

    // ------------------------------------------- BlockPlace Handling ---------------------------------------------- //

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPreBlockPlace(final BlockPlaceEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        // To prevent too fast scaffolding -> Timeout
        if (user.getTimeMap().at(TimeKey.SCAFFOLD_TIMEOUT).recentlyUpdated(timeout)) {
            event.setCancelled(true);
            InventoryUtil.syncUpdateInventory(user.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(final BlockPlaceEvent event)
    {
        final var user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        final var blockPlaced = event.getBlockPlaced();
        final var face = event.getBlock().getFace(event.getBlockAgainst());

        Log.finer(() -> "Scaffold-Debug | Player: %s placed block: %s against: %s on face: %s".formatted(user.getPlayer().getName(), blockPlaced.getType(), event.getBlockAgainst().getType(), face));
        Log.finer(() -> "Scaffold-Debug | Assumptions | Dist: %b, Fly: %b, Y: %b, Solid: %b, L/V: %b, Around: %b"
                .formatted(WorldUtil.INSTANCE.areLocationsInRange(user.getPlayer().getLocation(), blockPlaced.getLocation(), 4D),
                           !user.getPlayer().isFlying(),
                           user.getPlayer().getLocation().getY() > blockPlaced.getY(),
                           blockPlaced.getType().isSolid(),
                           event.getBlockPlaced().getType() != Material.LADDER && event.getBlockPlaced().getType() != Material.VINE,
                           WorldUtil.INSTANCE.countBlocksAround(blockPlaced, WorldUtil.ALL_FACES, MaterialUtil.INSTANCE.getLiquids()) == 1L));

        // Short distance between player and the block (at most 4 Blocks)
        if (WorldUtil.INSTANCE.areLocationsInRange(user.getPlayer().getLocation(), blockPlaced.getLocation(), 4D) &&
            // Not flying
            !user.getPlayer().isFlying() &&
            // Above the block
            user.getPlayer().getLocation().getY() > blockPlaced.getY() &&
            // Check if this check applies to the block
            blockPlaced.getType().isSolid() &&
            // Ladders and Vines are prone to false positives as they can be used to place blocks immediately after placing
            // them, therefore almost doubling the placement speed. However, they can only be placed one at a time, which
            // allows simply ignoring them.
            event.getBlockPlaced().getType() != Material.LADDER && event.getBlockPlaced().getType() != Material.VINE &&
            // Check if the block is placed against one block face only, also implies no blocks above and below.
            // Only one block that is not a liquid is allowed (the one which the Block is placed against).
            WorldUtil.INSTANCE.countBlocksAround(blockPlaced, WorldUtil.ALL_FACES, MaterialUtil.INSTANCE.getLiquids()) == 1L) {

            int vl = ScaffoldFace.INSTANCE.getVl(user, event);

            // In between check to make sure it is somewhat a scaffold movement as the buffering does not work.
            // Check that the player is not placing blocks up / down as that is not scaffolding.
            if (WorldUtil.HORIZONTAL_FACES.contains(face)) {
                final var lastScaffoldBlock = user.getScaffoldBatch().peekLastAdded().block();
                // This checks if the block was placed against the expected block for scaffolding.
                final var newScaffoldLocation = !Objects.equals(lastScaffoldBlock, event.getBlockAgainst()) || !WorldUtil.INSTANCE.isNext(lastScaffoldBlock, event.getBlockPlaced(), WorldUtil.HORIZONTAL_FACES);
                // ---------------------------------------------- Average ---------------------------------------------- //

                if (newScaffoldLocation) user.getScaffoldBatch().clear();

                user.getScaffoldBatch().addDataPoint(new ScaffoldBatch.ScaffoldBlockPlace(event.getBlockPlaced(),
                                                                                          face,
                                                                                          user));

                // --------------------------------------------- Rotations ---------------------------------------------- //

                vl += ScaffoldAngle.INSTANCE.getVl(user, event);
                vl += ScaffoldPosition.INSTANCE.getVl(event);

                // All these checks may have false positives in new situations.
                if (!newScaffoldLocation) {
                    // Do not check jumping for new locations as of wall-building / jumping.
                    vl += ScaffoldJumping.INSTANCE.getVl(user, event);
                    vl += ScaffoldRotation.INSTANCE.getVl(user);
                    vl += ScaffoldSafewalkEdge.INSTANCE.getVl(user, event);
                    vl += ScaffoldSafewalkTiming.INSTANCE.getVl(user);
                    vl += ScaffoldSprinting.INSTANCE.getVl(user);
                } else {
                    ScaffoldJumping.INSTANCE.newScaffoldLocation(user, event, lastScaffoldBlock);
                }
            }

            if (vl > 0) {
                this.getManagement().flag(Flag.of(event.getPlayer()).setAddedVl(vl).setCancelAction(cancelVl, () -> {
                    event.setCancelled(true);
                    user.getTimeMap().at(TimeKey.SCAFFOLD_TIMEOUT).update();
                    InventoryUtil.syncUpdateInventory(user.getPlayer());
                }));
            }
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        final var batchProcessor = new ScaffoldAverageBatchProcessor(this);
        return ModuleLoader.builder(this)
                           .batchProcessor(batchProcessor)
                           .build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).loadThresholdsToManagement().withDecay(80, 1).build();
    }
}
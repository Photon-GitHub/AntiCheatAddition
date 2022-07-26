package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.ModuleLoader;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.user.data.batch.ScaffoldBatch;
import de.photon.anticheataddition.util.inventory.InventoryUtil;
import de.photon.anticheataddition.util.minecraft.world.MaterialUtil;
import de.photon.anticheataddition.util.minecraft.world.WorldUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.Getter;
import lombok.val;
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

    private final ScaffoldAngle scaffoldAngle = new ScaffoldAngle(this.getConfigString());

    private final ScaffoldJumping scaffoldJumping = new ScaffoldJumping(this.getConfigString());

    private final ScaffoldPosition scaffoldPosition = new ScaffoldPosition(this.getConfigString());

    private final ScaffoldRotationFastChange scaffoldRotationFastChange = new ScaffoldRotationFastChange(this.getConfigString());
    private final ScaffoldRotationDerivative scaffoldRotationDerivative = new ScaffoldRotationDerivative(this.getConfigString());
    private final ScaffoldRotationSecondDerivative scaffoldRotationSecondDerivative = new ScaffoldRotationSecondDerivative(this.getConfigString());

    private final ScaffoldSafewalkPosition scaffoldSafewalkPosition = new ScaffoldSafewalkPosition(this.getConfigString());
    private final ScaffoldSafewalkTiming scaffoldSafewalkTiming = new ScaffoldSafewalkTiming(this.getConfigString());

    private final ScaffoldSprinting scaffoldSprinting = new ScaffoldSprinting(this.getConfigString());

    private final int cancelVl = loadInt(".cancel_vl", 110);
    private final int timeout = loadInt(".timeout", 1000);

    private Scaffold()
    {
        super("Scaffold");
    }

    // ------------------------------------------- BlockPlace Handling ---------------------------------------------- //

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPreBlockPlace(final BlockPlaceEvent event)
    {
        val user = User.getUser(event.getPlayer());
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
        val user = User.getUser(event.getPlayer());
        if (User.isUserInvalid(user, this)) return;

        val blockPlaced = event.getBlockPlaced();

        // Short distance between player and the block (at most 2 Blocks)
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
            WorldUtil.INSTANCE.countBlocksAround(blockPlaced, WorldUtil.ALL_FACES, MaterialUtil.LIQUIDS) == 1L &&
            // In between check to make sure it is somewhat a scaffold movement as the buffering does not work.
            WorldUtil.HORIZONTAL_FACES.contains(event.getBlock().getFace(event.getBlockAgainst())))
        {

            val lastScaffoldBlock = user.getScaffoldBatch().peekLastAdded().block();
            // This checks if the block was placed against the expected block for scaffolding.
            val newScaffoldLocation = !Objects.equals(lastScaffoldBlock, event.getBlockAgainst()) || !WorldUtil.INSTANCE.isNext(lastScaffoldBlock, event.getBlockPlaced(), WorldUtil.HORIZONTAL_FACES);

            // ---------------------------------------------- Average ---------------------------------------------- //

            if (newScaffoldLocation) user.getScaffoldBatch().clear();

            user.getScaffoldBatch().addDataPoint(new ScaffoldBatch.ScaffoldBlockPlace(event.getBlockPlaced(),
                                                                                      event.getBlockPlaced().getFace(event.getBlockAgainst()),
                                                                                      user));

            // --------------------------------------------- Rotations ---------------------------------------------- //

            int vl = this.scaffoldAngle.getVl(user, event);
            vl += this.scaffoldPosition.getVl(event);

            // All these checks may have false positives in new situations.
            if (!newScaffoldLocation) {
                // Do not check jumping for new locations as of wall-building / jumping.
                vl += this.scaffoldJumping.getVl(user, event);

                val angleInformation = user.getLookPacketData().getAngleInformation();

                val rotationVl = this.scaffoldRotationFastChange.getVl(user) +
                                 this.scaffoldRotationDerivative.getVl(user, angleInformation[0]) +
                                 this.scaffoldRotationSecondDerivative.getVl(user, angleInformation[1]);

                if (user.getData().counter.scaffoldRotationFails.conditionallyIncDec(rotationVl > 0)) vl += rotationVl;

                vl += this.scaffoldSafewalkPosition.getVl(user, event);
                vl += this.scaffoldSafewalkTiming.getVl(user);
                vl += this.scaffoldSprinting.getVl(user);
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
        val batchProcessor = new ScaffoldAverageBatchProcessor(this);
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
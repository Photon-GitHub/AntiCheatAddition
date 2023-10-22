package de.photon.anticheataddition.modules.checks.scaffold;


import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.messaging.Log;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This pattern checks for suspicious positions when placing a block to prevent extend scaffolds.
 */
final class ScaffoldJumping extends Module
{
    public static final ScaffoldJumping INSTANCE = new ScaffoldJumping();

    private static final int STAIR_SCAFFOLD_BYPASS_TIME = 2000;

    private ScaffoldJumping()
    {
        super("Scaffold.parts.Jumping");
    }

    public int getVl(User user, BlockPlaceEvent event)
    {
        if (!this.isEnabled()) return 0;

        final var failCounter = user.getData().counter.scaffoldJumpingFails;

        if (user.hasMovedRecently(TimeKey.XZ_MOVEMENT, 500)
            && user.hasJumpedRecently(1000)
            // Ignore stair scaffolding.
            && user.getTimeMap().at(TimeKey.SCAFFOLD_JUMPING_STAIR).notRecentlyUpdated(STAIR_SCAFFOLD_BYPASS_TIME)) {
            if (failCounter.incrementCompareThreshold()) {
                Log.fine(() -> "Scaffold-Debug | Player: " + event.getPlayer().getName() + " jumped while scaffolding.");
                return 20;
            }
        } else {
            // Decrease only every 18 blocks to make sure one cannot easily bypass this check by jumping only every other block.
            final var legitCounter = user.getData().counter.scaffoldJumpingLegit;
            if (legitCounter.incrementCompareThreshold()) {
                failCounter.decrementAboveZero();
                legitCounter.setToZero();
            }
        }

        return 0;
    }

    public void newScaffoldLocation(User user, BlockPlaceEvent event, Block lastPlacement)
    {
        if (!this.isEnabled()) return;

        Log.finer(() -> "Last placement for " + user.getPlayer().getName() + ": " + (lastPlacement == null ? " null " : lastPlacement.getY()) + " Current placement: " + event.getBlockPlaced().getY());

        // Ignore stair scaffolding.
        // Stair scaffolding would cause the last block to have a lower y value than the current block. The other parts of scaffold still work fine.
        if (lastPlacement != null && lastPlacement.getY() < event.getBlockPlaced().getY()) {
            Log.finer(() -> "Stair scaffolding for user " + user.getPlayer().getName() + " detected.");
            // Ignore for a short time as stair scaffolding has one block placement that is lower than the previous one, and then one that is of equal height.
            user.getTimeMap().at(TimeKey.SCAFFOLD_JUMPING_STAIR).update();
        }
    }
}

package de.photon.anticheataddition.modules.checks.scaffold;


import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.messaging.Log;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This pattern checks for suspicious positions when placing a block to prevent extend scaffolds.
 */
final class ScaffoldJumping extends Module
{
    public static final ScaffoldJumping INSTANCE = new ScaffoldJumping();

    private ScaffoldJumping()
    {
        super("Scaffold.parts.Jumping");
    }

    public int getVl(User user, BlockPlaceEvent event)
    {
        if (!this.isEnabled()) return 0;

        final var failCounter = user.getData().counter.scaffoldJumpingFails;

        if (user.hasMovedRecently(TimeKey.XZ_MOVEMENT, 500)
            && user.hasJumpedRecently(1000))
        {
            if (failCounter.incrementCompareThreshold()) {
                Log.fine(() -> "Scaffold-Debug | Player: " + event.getPlayer().getName() + " jumped while scaffolding.");
                return 20;
            }
        } else {
            // Decrease only every 20 blocks to make sure one cannot easily bypass this check by jumping only every other block.
            final var legitCounter = user.getData().counter.scaffoldJumpingLegit;
            if (legitCounter.incrementCompareThreshold()) {
                failCounter.decrementAboveZero();
                legitCounter.setToZero();
            }
        }
        return 0;
    }
}

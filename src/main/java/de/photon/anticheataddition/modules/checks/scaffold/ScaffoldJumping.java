package de.photon.anticheataddition.modules.checks.scaffold;


import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataKey;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.messaging.Log;
import lombok.Getter;
import lombok.val;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.function.ToIntBiFunction;

/**
 * This pattern checks for suspicious positions when placing a block to prevent extend scaffolds.
 */
final class ScaffoldJumping extends Module
{
    @Getter
    private ToIntBiFunction<User, BlockPlaceEvent> applyingConsumer = (user, event) -> 0;

    ScaffoldJumping(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Jumping");
    }

    @Override
    public void enable()
    {
        applyingConsumer = (user, event) -> {
            val failCounter = user.getDataMap().getCounter(DataKey.Count.SCAFFOLD_JUMPING_FAILS);

            if (user.hasMovedRecently(TimeKey.XZ_MOVEMENT, 500)
                && user.hasJumpedRecently(1000))
            {
                if (failCounter.incrementCompareThreshold()) {
                    Log.fine(() -> "Scaffold-Debug | Player: " + event.getPlayer().getName() + " jumped while scaffolding.");
                    return 20;
                }
            } else {
                // Decrease only every 10 blocks to make sure one cannot easily bypass this check by jumping only every other block.
                val legitCounter = user.getDataMap().getCounter(DataKey.Count.SCAFFOLD_JUMPING_LEGIT);
                if (legitCounter.incrementCompareThreshold()) {
                    failCounter.decrementAboveZero();
                    legitCounter.setToZero();
                }
            }
            return 0;
        };
    }

    @Override
    public void disable()
    {
        applyingConsumer = (user, event) -> 0;
    }
}

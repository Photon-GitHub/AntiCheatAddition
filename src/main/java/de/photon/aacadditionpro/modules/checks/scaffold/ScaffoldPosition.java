package de.photon.aacadditionpro.modules.checks.scaffold;

import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.mathematics.MathUtil;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import lombok.Getter;
import lombok.val;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.function.ToIntBiFunction;

/**
 * This pattern checks for suspicious positions when placing a block to prevent extend scaffolds.
 */
class ScaffoldPosition extends Module
{
    @Getter
    private ToIntBiFunction<User, BlockPlaceEvent> applyingConsumer = (user, event) -> 0;

    public ScaffoldPosition(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Position");
    }

    @Override
    public void enable()
    {
        applyingConsumer = (user, event) -> {
            // This sorts out scaffolding with non-full block hitboxes that will cause false positives (e.g. fences).
            if (!event.getBlockPlaced().getType().isOccluding()) return 0;

            val xOffset = MathUtil.absDiff(event.getPlayer().getLocation().getX(), event.getBlockAgainst().getX());
            val zOffset = MathUtil.absDiff(event.getPlayer().getLocation().getZ(), event.getBlockAgainst().getZ());

            boolean flag;
            switch (event.getBlock().getFace(event.getBlockAgainst())) {
                case EAST:
                    flag = xOffset <= 0;
                    break;
                case WEST:
                    flag = xOffset <= 1;
                    break;
                case NORTH:
                    flag = zOffset <= 1;
                    break;
                case SOUTH:
                    flag = zOffset <= 0;
                    break;
                default:
                    // Some other, mostly weird blockplaces.
                    flag = false;
                    break;
            }

            if (flag) {
                DebugSender.getInstance().sendDebug("Scaffold-Debug | Player: " + event.getPlayer().getName() + " placed from a suspicious location.");
                return 30;
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

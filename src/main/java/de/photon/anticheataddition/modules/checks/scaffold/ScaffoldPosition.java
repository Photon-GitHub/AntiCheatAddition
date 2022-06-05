package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.messaging.Log;
import lombok.val;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This pattern checks for suspicious positions when placing a block to prevent extend scaffolds.
 */
final class ScaffoldPosition extends Module
{
    ScaffoldPosition(String scaffoldConfigString)
    {
        super(scaffoldConfigString + ".parts.Position");
    }

    public int getVl(BlockPlaceEvent event)
    {
        if (!this.isEnabled()) return 0;

        // This sorts out scaffolding with non-full block hitboxes that will cause false positives (e.g. fences).
        if (!event.getBlockPlaced().getType().isOccluding()) return 0;

        val xOffset = MathUtil.absDiff(event.getPlayer().getLocation().getX(), event.getBlockAgainst().getX());
        val zOffset = MathUtil.absDiff(event.getPlayer().getLocation().getZ(), event.getBlockAgainst().getZ());

        boolean flag = switch (event.getBlock().getFace(event.getBlockAgainst())) {
            case EAST -> xOffset <= 0;
            case WEST -> xOffset <= 1;
            case NORTH -> zOffset <= 1;
            case SOUTH -> zOffset <= 0;
            // Some other, weird block placement.
            default -> false;
        };

        if (flag) {
            Log.fine(() -> "Scaffold-Debug | Player: " + event.getPlayer().getName() + " placed from a suspicious location.");
            return 30;
        }

        return 0;
    }
}

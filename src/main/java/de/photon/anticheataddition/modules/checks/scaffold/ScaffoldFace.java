package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.messaging.Log;
import de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.block.BlockPlaceEvent;

public class ScaffoldFace extends Module
{
    public static final ScaffoldFace INSTANCE = new ScaffoldFace();

    private ScaffoldFace()
    {
        super("Scaffold.parts.Face");
    }

    public int getVl(User user, BlockPlaceEvent event)
    {
        if (!this.isEnabled()) return 0;

        final Block blockAgainst = event.getBlockAgainst();

        if (MaterialUtil.INSTANCE.isAir(blockAgainst.getType()) || MaterialUtil.INSTANCE.isLiquid(blockAgainst.getType())) {
            Log.fine(() -> "Scaffold-Debug | Player: " + event.getPlayer().getName() + " placed block against air or liquid.");
            return 30;
        }

        final BlockFace face = event.getBlock().getFace(blockAgainst);
        if (face == null) {
            Log.fine(() -> "Scaffold-Debug | Player: " + event.getPlayer().getName() + " placed against distant block.");
            return 30;
        }

        return switch (face) {
            case UP, DOWN, NORTH, SOUTH, EAST, WEST -> 0;
            default -> {
                // Block placement against itself or diagonally is impossible in vanilla.
                Log.fine(() -> "Scaffold-Debug | Player: %s placed a block with a suspicious facing: %s".formatted(user.getPlayer().getName(), face));
                yield 30;
            }
        };
    }
}

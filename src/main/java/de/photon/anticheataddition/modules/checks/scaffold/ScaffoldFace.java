package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.messaging.Log;
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

        final BlockFace face = event.getBlock().getFace(event.getBlockAgainst());
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

package de.photon.anticheataddition.modules.checks.scaffold;

import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.messaging.Log;
import de.photon.anticheataddition.util.minecraft.world.material.MaterialUtil;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 * This pattern checks for suspicious positions when placing a block to prevent extend scaffolds.
 */
final class ScaffoldPosition extends Module
{
    public static final ScaffoldPosition INSTANCE = new ScaffoldPosition();

    private ScaffoldPosition()
    {
        super("Scaffold.parts.Position");
    }

    public int getVl(BlockPlaceEvent event)
    {
        if (!this.isEnabled()) return 0;

        if (MaterialUtil.INSTANCE.isAir(event.getBlockAgainst().getType()) || MaterialUtil.INSTANCE.getLiquids().contains(event.getBlockAgainst().getType())) {
            Log.fine(() -> "Scaffold-Debug | Player: " + event.getPlayer().getName() + " placed against air or liquid.");
            return 30;
        }

        // This sorts out scaffolding with non-full block hitboxes that will cause false positives (e.g. fences).
        if (!event.getBlockPlaced().getType().isOccluding()) return 0;

        final var face = event.getBlock().getFace(event.getBlockAgainst());
        if (face == null) {
            Log.fine(() -> "Scaffold-Debug | Player: " + event.getPlayer().getName() + " placed against distant block.");
            return 30;
        }

        final double xOffset = MathUtil.absDiff(event.getPlayer().getLocation().getX(), event.getBlockAgainst().getX());
        final double zOffset = MathUtil.absDiff(event.getPlayer().getLocation().getZ(), event.getBlockAgainst().getZ());

        Log.finer(() -> "Scaffold-Debug | Player: %s placed block with offsets x: %.3f z: %.3f on face: %s".formatted(event.getPlayer().getName(), xOffset, zOffset, face.name()));

        final boolean flag = switch (face) {
            case EAST -> xOffset <= 0;
            case WEST -> xOffset <= 1;
            case NORTH -> zOffset <= 1;
            case SOUTH -> zOffset <= 0;
            // Some other, weird block placement.
            default -> false;
        };

        if (flag) {
            Log.fine(() -> "Scaffold-Debug | Player: %s placed from a suspicious location. (Offsets x: %.3f, z: %.3f on face: %s)".formatted(event.getPlayer().getName(), xOffset, zOffset, face.name()));
            return 30;
        }

        return 0;
    }
}

package de.photon.AACAdditionPro.util.entities.movement;

import lombok.AllArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

@AllArgsConstructor
public class BasicMovement extends Movement
{
    private final Player player;
    private double entityOffset;
    private double offsetRandomizationRange;
    private double minXZDifference;

    @Override
    public Location calculate(Location old)
    {
        // Spawning-Location
        final Location moveLocation = player.getLocation();
        final double origX = moveLocation.getX();
        final double origZ = moveLocation.getZ();

        // Move behind the player to make the entity not disturb players
        // Important: the negative offset!
        moveLocation.add(moveLocation.getDirection().setY(0).normalize().multiply(-(entityOffset + ThreadLocalRandom.current().nextDouble(offsetRandomizationRange))));

        final double currentXZDifference = Math.hypot(moveLocation.getX() - origX, moveLocation.getZ() - origZ);

        if (currentXZDifference < minXZDifference) {
            final double radiansYaw = Math.toRadians(moveLocation.getYaw());

            final Vector moveAddVector = new Vector(-Math.sin(radiansYaw), 0, Math.cos(radiansYaw));
            moveLocation.add(moveAddVector.normalize().multiply(-(minXZDifference - currentXZDifference)));
        }

        old.setX(moveLocation.getX());
        old.setZ(moveLocation.getZ());
        return old;
    }
}

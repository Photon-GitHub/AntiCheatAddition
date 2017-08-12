package de.photon.AACAdditionPro.util.entities.movement;

import lombok.AllArgsConstructor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author geNAZt
 * @version 1.0
 */
@AllArgsConstructor
public class BasicMovement extends Movement
{
    private final Player player;
    private double entityOffset;
    private double offsetRandomizationRange;
    private double minXZDifference;

    @Override
    public Location calculate()
    {
        // Spawning-Location
        final Location location = player.getLocation();
        final double origX = location.getX();
        final double origZ = location.getZ();

        // Move behind the player to make the entity not disturb players
        // Important: the negative offset!
        location.add(location.getDirection().setY(0).normalize().multiply(-(entityOffset + ThreadLocalRandom.current().nextDouble(offsetRandomizationRange))));

        final double currentXZDifference = Math.hypot(location.getX() - origX, location.getZ() - origZ);

        if (currentXZDifference < minXZDifference) {
            final Vector moveAddVector = new Vector(-Math.sin(Math.toRadians(location.getYaw())), 0, Math.cos(Math.toRadians(location.getYaw())));
            location.add(moveAddVector.normalize().multiply(-(minXZDifference - currentXZDifference)));
        }

        return location;
    }
}

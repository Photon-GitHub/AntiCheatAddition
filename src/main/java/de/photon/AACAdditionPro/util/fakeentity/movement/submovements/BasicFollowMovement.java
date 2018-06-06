package de.photon.AACAdditionPro.util.fakeentity.movement.submovements;

import de.photon.AACAdditionPro.api.killauraentity.MovementType;
import de.photon.AACAdditionPro.util.fakeentity.movement.Movement;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BasicFollowMovement implements Movement
{
    private final Player player;
    private double entityOffset;
    private double offsetRandomizationRange;
    private double minXZDifference;
    private boolean isTPNeeded;
    private boolean shouldSprint;

    public BasicFollowMovement(Player player, double entityOffset, double offsetRandomizationRange, double minXZDifference)
    {
        this.player = player;
        this.entityOffset = entityOffset;
        this.offsetRandomizationRange = offsetRandomizationRange;
        this.minXZDifference = minXZDifference;
    }

    @Override
    public Vector calculate(Location old)
    {
        // Spawning-Location
        final Location moveLocation = player.getLocation();
        final double origX = moveLocation.getX();
        final double origZ = moveLocation.getZ();

        // Move behind the player to make the entity not disturb players
        // Important: the negative offset!
        moveLocation.add(moveLocation.getDirection().setY(0).normalize().multiply(-(MathUtils.randomBoundaryDouble(entityOffset, offsetRandomizationRange))));

        final double currentXZDifference = Math.hypot(moveLocation.getX() - origX, moveLocation.getZ() - origZ);

        if (currentXZDifference < minXZDifference)
        {
            final double radiansYaw = Math.toRadians(moveLocation.getYaw());

            final Vector moveAddVector = new Vector(-Math.sin(radiansYaw), 0, Math.cos(radiansYaw));
            moveLocation.add(moveAddVector.normalize().multiply(-(minXZDifference - currentXZDifference)));
        }

        final Vector movementVector = new Vector(moveLocation.getX() - old.getX(), 0, moveLocation.getZ() - old.getZ());

        isTPNeeded = movementVector.lengthSquared() > 49;
        shouldSprint = !isTPNeeded && movementVector.lengthSquared() > 16;

        return movementVector;
    }

    @Override
    public MovementType getMovementType()
    {
        return MovementType.BASIC_FOLLOW;
    }

    @Override
    public boolean shouldSprint()
    {
        return shouldSprint;
    }

    @Override
    public boolean jumpIfCollidedHorizontally()
    {
        return true;
    }

    @Override
    public boolean isTPNeeded()
    {
        return isTPNeeded;
    }
}

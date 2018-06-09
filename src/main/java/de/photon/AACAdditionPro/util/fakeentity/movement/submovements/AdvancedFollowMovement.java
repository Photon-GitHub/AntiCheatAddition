package de.photon.AACAdditionPro.util.fakeentity.movement.submovements;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.api.killauraentity.MovementType;
import de.photon.AACAdditionPro.util.fakeentity.movement.Movement;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AdvancedFollowMovement implements Movement
{
    private final Player player;

    private double entityOffset = AACAdditionPro.getInstance().getConfig().getDouble(ModuleType.KILLAURA_ENTITY.getConfigString() + ".position.entityOffset");
    private double offsetRandomizationRange = AACAdditionPro.getInstance().getConfig().getDouble(ModuleType.KILLAURA_ENTITY.getConfigString() + ".position.offsetRandomizationRange");

    private boolean isTPNeeded;
    private boolean shouldSprint;

    public AdvancedFollowMovement(Player player)
    {
        this.player = player;
    }

    @Override
    public Vector calculate(Location old)
    {
        // Spawning-Location
        // player.getLocation already returns a cloned location.
        final Location observedPlayerLocation = player.getLocation();

        // Create copy of the location to work with.
        final Location playerWorkLocation = observedPlayerLocation.clone();

        // Forward facing to make sure the movement calculation works.
        playerWorkLocation.setPitch(0);

        // Move behind the player to make the entity not disturb players
        // Make sure the entity is behind the player -> use the direction vector
        playerWorkLocation.add(playerWorkLocation.getDirection()
                                                 // No normalization as getDirection() already returns a normalized vector.
                                                 .multiply(
                                                         // Negative offset to make sure the entity is behind the player.
                                                         -(MathUtils.randomBoundaryDouble(entityOffset, offsetRandomizationRange))
                                                          ));

       /* final double currentXZDifference = Math.hypot(observedPlayerLocation.getX() - origX, observedPlayerLocation.getZ() - origZ);

        if (currentXZDifference < minXZDifference)
        {
            final double radiansYaw = Math.toRadians(observedPlayerLocation.getYaw());

            final Vector moveAddVector = new Vector(-Math.sin(radiansYaw), 0, Math.cos(radiansYaw));
            observedPlayerLocation.add(moveAddVector.normalize().multiply(-(minXZDifference - currentXZDifference)));
        }*/

        final Vector movementVector = playerWorkLocation.subtract(old).toVector();

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

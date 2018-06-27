package de.photon.AACAdditionPro.util.fakeentity.movement.submovements;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.api.killauraentity.Movement;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import org.bukkit.Location;

public class BasicFollowMovement implements Movement
{
    private final double entityOffset = AACAdditionPro.getInstance().getConfig().getDouble(ModuleType.KILLAURA_ENTITY.getConfigString() + ".position.entityOffset");
    private double offsetRandomizationRange = AACAdditionPro.getInstance().getConfig().getDouble(ModuleType.KILLAURA_ENTITY.getConfigString() + ".position.offsetRandomizationRange");

    private boolean isTPNeeded;
    private boolean shouldSprint;

    @Override
    public Location calculate(Location playerLocation, Location old)
    {
        // Create copy of the location to work with.
        final Location playerWorkLocation = playerLocation.clone();

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

        final double lengthSquared = Math.max(old.distanceSquared(playerWorkLocation), playerLocation.distanceSquared(playerWorkLocation));
        isTPNeeded = lengthSquared > 64;
        shouldSprint = !isTPNeeded && lengthSquared > 25;

        return playerWorkLocation;
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

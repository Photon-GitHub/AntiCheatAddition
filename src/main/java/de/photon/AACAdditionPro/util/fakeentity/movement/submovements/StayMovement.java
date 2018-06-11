package de.photon.AACAdditionPro.util.fakeentity.movement.submovements;

import de.photon.AACAdditionPro.api.killauraentity.MovementType;
import de.photon.AACAdditionPro.util.fakeentity.movement.Movement;
import org.bukkit.Location;

public class StayMovement implements Movement
{
    @Override
    public Location calculate(Location playerLocation, Location old)
    {
        return old.clone();
    }

    @Override
    public MovementType getMovementType()
    {
        return MovementType.STAY;
    }

    @Override
    public boolean shouldSprint()
    {
        return false;
    }

    @Override
    public boolean jumpIfCollidedHorizontally()
    {
        return false;
    }

    @Override
    public boolean isTPNeeded()
    {
        return false;
    }
}

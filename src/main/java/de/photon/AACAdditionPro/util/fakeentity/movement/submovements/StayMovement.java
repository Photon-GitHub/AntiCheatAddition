package de.photon.AACAdditionPro.util.fakeentity.movement.submovements;

import de.photon.AACAdditionPro.api.killauraentity.Movement;
import org.bukkit.Location;

public class StayMovement implements Movement
{
    @Override
    public Location calculate(Location playerLocation, Location old)
    {
        return old.clone();
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

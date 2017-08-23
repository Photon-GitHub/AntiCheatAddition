package de.photon.AACAdditionPro.util.entities.movement.submovements;

import de.photon.AACAdditionPro.api.killauraentity.MovementType;
import de.photon.AACAdditionPro.util.entities.movement.Movement;
import org.bukkit.Location;
import org.bukkit.util.Vector;

public class StayMovement implements Movement
{
    @Override
    public Vector calculate(Location old)
    {
        return new Vector();
    }

    @Override
    public MovementType getMovementType()
    {
        return MovementType.STAY;
    }

    @Override
    public boolean jumpIfCollidedHorizontally()
    {
        return false;
    }
}

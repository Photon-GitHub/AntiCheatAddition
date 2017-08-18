package de.photon.AACAdditionPro.util.entities.movement.submovements;

import de.photon.AACAdditionPro.api.killauraentity.MovementType;
import de.photon.AACAdditionPro.util.entities.movement.Movement;
import org.bukkit.Location;

public class StayMovement implements Movement
{
    @Override
    public Location calculate(Location old)
    {
        return old;
    }

    @Override
    public MovementType getMovementType()
    {
        return MovementType.STAY;
    }
}

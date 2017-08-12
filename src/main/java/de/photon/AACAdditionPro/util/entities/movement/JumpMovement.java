package de.photon.AACAdditionPro.util.entities.movement;

import de.photon.AACAdditionPro.util.entities.ClientsidePlayerEntity;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;

/**
 * @author geNAZt
 * @version 1.0
 *
 * Thats just a legit jump from a 1.8.8 client as reference data:
 *
 * MotY: 0.33319999363422365
 * MotY: 0.24813599859094576
 * MotY: 0.16477328182606651
 * MotY: 0.08307781780646721
 * MotY: 0.0030162615090425808
 * MotY: -0.0784000015258789
 * MotY: -0.1552320045166016
 * MotY: -0.230527368912964
 * MotY: -0.30431682745754424
 * MotY: -0.37663049823865513
 * MotY: -0.44749789698341763
 */
@RequiredArgsConstructor
public class JumpMovement extends Movement
{
    private final ClientsidePlayerEntity entity;

    // We need to send 11 movements for a jump
    private int sentMovements = 0;

    private double currentMotionY;

    @Override
    public Location calculate()
    {
        // Did we start the jump?
        if ( this.sentMovements == 0 ) {
            this.currentMotionY = Jumping.getJumpYMotion( (short) 0 );
            this.sentMovements++;
        }

        this.currentMotionY -= 0.08; // Gravitation
        this.currentMotionY *= 0.98;
        this.sentMovements++;

        if ( this.sentMovements == 13 ) {
            this.sentMovements = 0;
            return null;
        }

        System.out.println( this.currentMotionY );
        return this.entity.getLocation().add( 0, this.currentMotionY, 0 );
    }
}

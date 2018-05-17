package de.photon.AACAdditionPro.util.datawrappers;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.util.fakeentity.movement.Gravitation;
import de.photon.AACAdditionPro.util.fakeentity.movement.Jumping;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public class TowerBlockPlace extends BlockPlace
{
    private static final double TOWER_LENIENCY = AACAdditionPro.getInstance().getConfig().getDouble(ModuleType.TOWER + ".tower_leniency");
    private static final double LEVITATION_LENIENCY = AACAdditionPro.getInstance().getConfig().getDouble(ModuleType.TOWER + ".levitation_leniency");
    private static final double[] AMPLIFIER_CHACHE = {
            // 478.4 * 0.925
            442.52D,
            // 578.4 * 0.925
            542.52D,
            // 290 * 0.925
            268.25,
            // 190 * 0.925
            175.75,
            // 140 * 0.925
            129.5
    };

    private final Integer jumpBoostLevel;
    private final Integer levitationLevel;

    public TowerBlockPlace(Block block, Integer jumpBoostLevel, Integer levitationLevel)
    {
        super(block);
        this.jumpBoostLevel = jumpBoostLevel;
        this.levitationLevel = levitationLevel;
    }

    /**
     * Calculates the time needed to place one block.
     */
    public double calculateDelay()
    {
        if (levitationLevel != null)
        {
            // 0.9 Blocks per second per levitation level.
            return 900 / (levitationLevel + 1) * TOWER_LENIENCY * LEVITATION_LENIENCY;
        }

        // No JUMP_BOOST
        if (jumpBoostLevel == null)
        {
            return AMPLIFIER_CHACHE[0];
        }

        // Player has JUMP_BOOST
        if (jumpBoostLevel < 0)
        {
            // Negative JUMP_BOOST -> Not allowed to place blocks -> Very high delay
            return 1500;
        }

        if (jumpBoostLevel + 1 < AMPLIFIER_CHACHE.length)
        {
            return AMPLIFIER_CHACHE[jumpBoostLevel + 1];
        }

        // How many blocks can potentially be placed during one jump cycle
        short maximumPlacedBlocks = 1;

        // The velocity in the beginning
        Vector currentVelocity = new Vector(0, Jumping.getJumpYMotion(jumpBoostLevel), 0);

        // The first tick (1) happens here
        double currentBlockValue = currentVelocity.getY();

        // Start the tick-loop at 2 due to the one tick outside.
        for (short ticks = 2; ticks < 160; ticks++)
        {
            currentVelocity = Gravitation.applyGravitationAndAirResistance(currentVelocity, Gravitation.PLAYER);

            currentBlockValue += currentVelocity.getY();

            // The maximum placed blocks are the next lower integer of the maximum y-Position of the player
            final short flooredBlocks = (short) Math.floor(currentBlockValue);
            if (maximumPlacedBlocks < flooredBlocks)
            {
                maximumPlacedBlocks = flooredBlocks;
            }
            else
            {
                // Location must be lower than maximumPlacedBlocks and there is negative velocity (in the beginning there is no negative velocity, but maximumPlacedBlocks > flooredBlocks!)
                if (maximumPlacedBlocks > flooredBlocks && currentVelocity.getY() < 0)
                {
                    // Leniency:
                    double leniency;
                    switch (jumpBoostLevel)
                    {
                        case 0:
                            leniency = 1;
                            break;
                        case 1:
                        case 3:
                            leniency = 0.9;
                            break;
                        case 2:
                            leniency = 0.87;
                            break;
                        default:
                            leniency = 0.982;
                            break;
                    }

                    // If the result is lower here, the detection is more lenient.
                    // Convert ticks to milliseconds
                    return ((ticks * 50) / maximumPlacedBlocks) * leniency * TOWER_LENIENCY;
                }
            }
        }

        // Too high movement; no checking
        return 0;
    }
}

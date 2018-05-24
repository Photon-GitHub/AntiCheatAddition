package de.photon.AACAdditionPro.util.datawrappers;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.util.fakeentity.movement.Gravitation;
import de.photon.AACAdditionPro.util.fakeentity.movement.Jumping;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

public class TowerBlockPlace extends BlockPlace
{
    private static final double TOWER_LENIENCY = AACAdditionPro.getInstance().getConfig().getDouble(ModuleType.TOWER.getConfigString() + ".tower_leniency");
    private static final double LEVITATION_LENIENCY = AACAdditionPro.getInstance().getConfig().getDouble(ModuleType.TOWER.getConfigString() + ".levitation_leniency");
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

        // The velocity in the beginning
        Vector currentVelocity = new Vector(0, Jumping.getJumpYMotion(jumpBoostLevel), 0);

        // The first tick (1) happens here
        double currentBlockValue = currentVelocity.getY();

        // Start the tick-loop at 2 due to the one tick outside.
        for (short ticks = 2; ticks < 160; ticks++)
        {
            currentVelocity = Gravitation.applyGravitationAndAirResistance(currentVelocity, Gravitation.PLAYER);

            // Break as the player has already reached the max height (no more blocks to place below).
            if (currentVelocity.getY() <= 0)
            {
                // If the result is lower here, the detection is more lenient.
                // * 50 : Convert ticks to milliseconds
                // 0.925 is additional leniency
                // Deliberately ignore the "falling" phase above the last block to increase leniency and code simplicity
                return ((ticks * 50) / Math.floor(currentBlockValue)) * 0.925 * TOWER_LENIENCY;
            }

            currentBlockValue += currentVelocity.getY();
        }

        // Too high movement; no checking
        return 0;
    }
}

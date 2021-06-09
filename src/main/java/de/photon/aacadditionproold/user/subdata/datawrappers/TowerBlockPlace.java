package de.photon.aacadditionproold.user.subdata.datawrappers;

import com.google.common.collect.ImmutableList;
import de.photon.aacadditionproold.AACAdditionPro;
import de.photon.aacadditionproold.modules.ModuleType;
import de.photon.aacadditionproold.util.simulation.Gravitation;
import de.photon.aacadditionproold.util.simulation.Jumping;
import org.bukkit.block.Block;
import org.bukkit.util.Vector;

import java.util.List;

public class TowerBlockPlace extends BlockPlace
{
    final Integer jumpBoostLevel;
    final Integer levitationLevel;

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
        if (this.levitationLevel != null) {
            // 0.9 Blocks per second per levitation level.
            return (900 / (this.levitationLevel + 1D)) * TOWER_LENIENCY * LEVITATION_LENIENCY;
        }

        // No JUMP_BOOST
        if (this.jumpBoostLevel == null) {
            return AMPLIFIER_CACHE.get(0);
        }

        // Player has JUMP_BOOST
        if (this.jumpBoostLevel < 0) {
            // Negative JUMP_BOOST -> Not allowed to place blocks -> Very high delay
            return 1500;
        }

        if (this.jumpBoostLevel + 1 < AMPLIFIER_CACHE.size()) {
            return AMPLIFIER_CACHE.get(this.jumpBoostLevel + 1);
        }

        // The velocity in the beginning
        Vector currentVelocity = new Vector(0, Jumping.getJumpYMotion(this.jumpBoostLevel), 0);

        // The first tick (1) happens here
        double currentBlockValue = currentVelocity.getY();
        Double landingBlock = null;

        // Start the tick-loop at 2 due to the one tick outside.
        for (short ticks = 2; ticks < 160; ++ticks) {
            currentVelocity = Gravitation.applyGravitationAndAirResistance(currentVelocity, Gravitation.PLAYER);

            // Break as the player has already reached the max height (no more blocks to place below).
            if (currentVelocity.getY() <= 0) {
                if (landingBlock == null) {
                    landingBlock = Math.floor(currentBlockValue);
                } else if (currentBlockValue <= landingBlock) {
                    // If the result is lower here, the detection is more lenient.
                    // * 50 : Convert ticks to milliseconds
                    // 0.92 is the required simulation leniency I got from testing.
                    // 0.925 is additional leniency
                    // -15 is special leniency for high jump boost environments.
                    return ((((ticks * 50) / landingBlock) * 0.92 * 0.925) - 15) * TOWER_LENIENCY;
                }
            }

            currentBlockValue += currentVelocity.getY();
        }

        // Too high movement; no checking
        return 0;
    }
}

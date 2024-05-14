package de.photon.anticheataddition.util.minecraft.movement;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MovementTest
{
    @Test
    void testApplyGravitation()
    {
        Vector vector = new Vector(0, 1, 0);

        Vector result = Movement.PLAYER.applyGravitation(vector.clone());
        assertEquals(0.92, result.getY(), 0.001);

        result = Movement.PLAYER_SLOW_FALLING.applyGravitation(vector.clone());
        assertEquals(0.99, result.getY(), 0.001);

        result = Movement.FALLING_ITEMS_BLOCKS.applyGravitation(vector.clone());
        assertEquals(0.96, result.getY(), 0.001);

        result = Movement.PROJECTILES.applyGravitation(vector.clone());
        assertEquals(0.97, result.getY(), 0.001);
    }

    @Test
    void testApplyAirResistance()
    {
        Vector vector = new Vector(1, 1, 1);

        Vector result = Movement.PLAYER.applyAirResistance(vector.clone());
        assertEquals(0.98, result.getX(), 0.001);
        assertEquals(0.98, result.getY(), 0.001);
        assertEquals(0.98, result.getZ(), 0.001);
    }

    @Test
    void testApplyGravitationAndAirResistance()
    {
        Vector vector = new Vector(0, 1, 0);

        Vector result = Movement.PLAYER.applyGravitationAndAirResistance(vector.clone());
        assertEquals(0.98 * 0.92, result.getY(), 0.001);

        result = Movement.PLAYER_SLOW_FALLING.applyGravitationAndAirResistance(vector.clone());
        assertEquals(0.98 * 0.99, result.getY(), 0.001);

        result = Movement.FALLING_ITEMS_BLOCKS.applyGravitationAndAirResistance(vector.clone());
        assertEquals(0.98 * 0.96, result.getY(), 0.001);

        result = Movement.PROJECTILES.applyGravitationAndAirResistance(vector.clone());
        assertEquals(0.98 * 0.97, result.getY(), 0.001);
    }

    @Test
    void testGetJumpYMotion()
    {
        assertThrows(UnsupportedOperationException.class, () -> Movement.PROJECTILES.getJumpYMotion(null));

        assertEquals(0.42, Movement.PLAYER.getJumpYMotion(null), 0.001);
        assertEquals(0.62, Movement.PLAYER.getJumpYMotion(1), 0.001);

        assertEquals(0.42, Movement.PLAYER_SLOW_FALLING.getJumpYMotion(null), 0.001);
        assertEquals(0.62, Movement.PLAYER_SLOW_FALLING.getJumpYMotion(1), 0.001);
    }
}

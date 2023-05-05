package de.photon.anticheataddition.util.mathematics;

import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResetVectorTest
{
    @Test
    void testInit()
    {
        final var numberInit = new ResetVector(1.0, 2.0, 3.0);
        assertEquals(1.0, numberInit.getX());
        assertEquals(2.0, numberInit.getY());
        assertEquals(3.0, numberInit.getZ());

        final var vectorInit = new ResetVector(new Vector(1.0, 2.0, 3.0));
        assertEquals(1.0, vectorInit.getX());
        assertEquals(2.0, vectorInit.getY());
        assertEquals(3.0, vectorInit.getZ());
    }

    @Test
    void testResetToBase()
    {
        final var resetVector = new ResetVector(1.0, 2.0, 3.0);
        final var resetVector2 = new ResetVector(resetVector);

        resetVector.setX(4.0);
        resetVector2.setY(25);

        assertEquals(4.0, resetVector.getX());
        assertEquals(2.0, resetVector.getY());
        assertEquals(3.0, resetVector.getZ());

        assertEquals(1.0, resetVector2.getX());
        assertEquals(25.0, resetVector2.getY());
        assertEquals(3.0, resetVector2.getZ());

        resetVector2.resetToBase();
        assertEquals(1.0, resetVector2.getX());
        assertEquals(2.0, resetVector2.getY());
        assertEquals(3.0, resetVector2.getZ());
    }
}

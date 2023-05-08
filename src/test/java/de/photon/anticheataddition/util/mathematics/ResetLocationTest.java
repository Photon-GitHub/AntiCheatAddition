package de.photon.anticheataddition.util.mathematics;

import org.bukkit.Location;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResetLocationTest
{
    @Test
    void testInit()
    {
        final var noArgInit = new ResetLocation();
        assertEquals(0.0, noArgInit.getX());
        assertEquals(0.0, noArgInit.getY());
        assertEquals(0.0, noArgInit.getZ());

        final var numberInit = new ResetLocation(null, 1.0, 2.0, 3.0);
        assertEquals(1.0, numberInit.getX());
        assertEquals(2.0, numberInit.getY());
        assertEquals(3.0, numberInit.getZ());

        final var vectorInit = new ResetLocation(new Location(null, 1.0, 2.0, 3.0));
        assertEquals(1.0, vectorInit.getX());
        assertEquals(2.0, vectorInit.getY());
        assertEquals(3.0, vectorInit.getZ());
    }

    @Test
    void testResetToBase()
    {
        final var resetLocation = new ResetLocation(null, 1.0, 2.0, 3.0);
        final var resetLocation2 = new ResetLocation(resetLocation);

        resetLocation.setX(4.0);
        resetLocation2.setY(25);

        assertEquals(4.0, resetLocation.getX());
        assertEquals(2.0, resetLocation.getY());
        assertEquals(3.0, resetLocation.getZ());

        assertEquals(1.0, resetLocation2.getX());
        assertEquals(25.0, resetLocation2.getY());
        assertEquals(3.0, resetLocation2.getZ());

        resetLocation2.resetToBase();
        assertEquals(1.0, resetLocation2.getX());
        assertEquals(2.0, resetLocation2.getY());
        assertEquals(3.0, resetLocation2.getZ());
    }
}

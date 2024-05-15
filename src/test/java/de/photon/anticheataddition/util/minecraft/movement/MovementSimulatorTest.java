package de.photon.anticheataddition.util.minecraft.movement;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovementSimulatorTest
{

    private Location location;
    private Vector velocity;

    @BeforeEach
    void setUp()
    {
        location = new Location(null, 0, 0, 0);
        velocity = new Vector(1, 1, 1);
    }

    @Test
    void testConstructor()
    {
        MovementSimulator simulator = new MovementSimulator(location, velocity, Movement.PLAYER);

        assertEquals(location, simulator.getCurrent());
        assertEquals(velocity, simulator.getVelocity());
        assertEquals(Movement.PLAYER, simulator.getMovement());
        assertEquals(0, simulator.getTick());
    }

    @Test
    void testTick()
    {
        MovementSimulator simulator = new MovementSimulator(location, velocity, Movement.PLAYER);

        simulator.tick();

        assertEquals(0.98, simulator.getVelocity().getX(), 0.001);
        assertEquals(0.98 * (1 - 0.08), simulator.getVelocity().getY(), 0.001); // Apply gravity then air resistance
        assertEquals(0.98, simulator.getVelocity().getZ(), 0.001);

        assertEquals(1.0, simulator.getCurrent().getX(), 0.001);
        assertEquals(1.0, simulator.getCurrent().getY(), 0.001);
        assertEquals(1.0, simulator.getCurrent().getZ(), 0.001);

        assertEquals(1, simulator.getTick());
    }

    @Test
    void testTickUntil()
    {
        MovementSimulator simulator = new MovementSimulator(location, velocity, Movement.PLAYER);

        simulator.tickUntil(sim -> sim.getTick() == 2, 10);

        final double velX = 0.98 * 0.98;
        final double locX = 1.0 + 0.98;

        double velY = (1.0 - 0.08) * 0.98;
        final double locY = 1.0 + velY;
        velY = (velY - 0.08) * 0.98;

        final double velZ = 0.98 * 0.98;
        final double locZ = 1.0 + 0.98;

        assertEquals(velX, simulator.getVelocity().getX(), 0.001);
        assertEquals(velY, simulator.getVelocity().getY(), 0.001); // Apply gravity then air resistance
        assertEquals(velZ, simulator.getVelocity().getZ(), 0.001);

        assertEquals(locX, simulator.getCurrent().getX(), 0.001);
        assertEquals(locY, simulator.getCurrent().getY(), 0.001);
        assertEquals(locZ, simulator.getCurrent().getZ(), 0.001);

        assertEquals(2, simulator.getTick());
    }

    @Test
    void testSetVelocity()
    {
        MovementSimulator simulator = new MovementSimulator(location, velocity, Movement.PLAYER);

        Vector newVelocity = new Vector(2, 2, 2);
        simulator.setVelocity(newVelocity);

        assertEquals(newVelocity, simulator.getVelocity());
    }

    @Test
    void testTickWithPlayerSlowFalling()
    {
        MovementSimulator simulator = new MovementSimulator(location, velocity, Movement.PLAYER_SLOW_FALLING);

        simulator.tick();

        assertEquals(0.98, simulator.getVelocity().getX(), 0.001);
        assertEquals(0.98 * (1 - 0.01), simulator.getVelocity().getY(), 0.001); // Apply gravity then air resistance
        assertEquals(0.98, simulator.getVelocity().getZ(), 0.001);

        assertEquals(1.0, simulator.getCurrent().getX(), 0.001);
        assertEquals(1.0, simulator.getCurrent().getY(), 0.001);
        assertEquals(1.0, simulator.getCurrent().getZ(), 0.001);

        assertEquals(1, simulator.getTick());
    }

    @Test
    void testTickWithProjectile()
    {
        MovementSimulator simulator = new MovementSimulator(location, velocity, Movement.PROJECTILES);

        simulator.tick();

        assertEquals(0.98, simulator.getVelocity().getX(), 0.001);
        assertEquals(0.98 * (1 - 0.03), simulator.getVelocity().getY(), 0.001); // Apply gravity then air resistance
        assertEquals(0.98, simulator.getVelocity().getZ(), 0.001);

        assertEquals(1.0, simulator.getCurrent().getX(), 0.001);
        assertEquals(1.0, simulator.getCurrent().getY(), 0.001);
        assertEquals(1.0, simulator.getCurrent().getZ(), 0.001);

        assertEquals(1, simulator.getTick());
    }
}

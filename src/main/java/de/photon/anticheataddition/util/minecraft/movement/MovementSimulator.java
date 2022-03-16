package de.photon.anticheataddition.util.minecraft.movement;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.function.Predicate;

@Getter
@ToString
public class MovementSimulator
{
    private final Location current;
    private final Movement movement;
    @Setter private Vector velocity;
    private int tick = 0;

    public MovementSimulator(Location current, Vector velocity, Movement movement)
    {
        this.current = current;
        this.velocity = velocity;
        this.movement = movement;
    }

    public void tickUntil(Predicate<MovementSimulator> condition, int maxTicks)
    {
        final int lastTick = tick + maxTicks;
        while (tick < lastTick && !condition.test(this)) this.tick();
    }

    public void tick()
    {
        current.add(velocity);
        movement.applyGravitationAndAirResistance(velocity);
        ++tick;
    }
}

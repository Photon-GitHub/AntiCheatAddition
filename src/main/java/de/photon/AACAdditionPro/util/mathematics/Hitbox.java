package de.photon.AACAdditionPro.util.mathematics;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;

public enum Hitbox
{
    PLAYER(0.3D, 1.8D),
    SNEAKING_PLAYER(0.3D, 1.65D);

    private final double offset;
    private final double height;

    Hitbox(final double offset, final double height)
    {
        this.offset = offset;
        this.height = height;
    }

    public static ArrayList<Vector> getCalculationVectors(final Hitbox hitbox, final Location location)
    {
        final ArrayList<Vector> vectors = new ArrayList<>(11);
        final Vector start = location.toVector();

        //Lower corners
        vectors.add(new Vector(start.getX() + hitbox.offset, start.getY(), start.getZ() + hitbox.offset));
        vectors.add(new Vector(start.getX() - hitbox.offset, start.getY(), start.getZ() + hitbox.offset));
        vectors.add(new Vector(start.getX() + hitbox.offset, start.getY(), start.getZ() - hitbox.offset));
        vectors.add(new Vector(start.getX() - hitbox.offset, start.getY(), start.getZ() - hitbox.offset));

        //Upper corners
        vectors.add(new Vector(start.getX() + hitbox.offset, start.getY() + hitbox.height, start.getZ() + hitbox.offset));
        vectors.add(new Vector(start.getX() - hitbox.offset, start.getY() + hitbox.height, start.getZ() + hitbox.offset));
        vectors.add(new Vector(start.getX() + hitbox.offset, start.getY() + hitbox.height, start.getZ() - hitbox.offset));
        vectors.add(new Vector(start.getX() - hitbox.offset, start.getY() + hitbox.height, start.getZ() - hitbox.offset));

        final double step_size;
        if (location.getY() - location.getBlockX() > 0.1) {
            // Steps and other blocks with irregular hitboxes need more steps (below 0.5 blocks)
            step_size = 0.47D;
        } else {
            step_size = 1D;
        }

        for (double d = 1.47; d < hitbox.height - 1; d += step_size) {
            vectors.add(new Vector(start.getX(), start.getY() + d, start.getZ()));
        }
        return vectors;
    }

    /**
     * Gets the {@link Location} of an {@link Entity} and adds the maximum height of it's {@link Hitbox}
     */
    public static Location getLocationWithHeight(final Entity entity)
    {

        if (entity instanceof Player) {
            final Player player = (Player) entity;
            return entity.getLocation().add(0, player.isSneaking() ?
                                               (SNEAKING_PLAYER.getHeight()) :
                                               (PLAYER.getHeight()), 0);
        }

        return entity.getLocation().add(0, entity.getHeight(), 0);
    }


    /**
     * Gets the middle of an {@link org.bukkit.entity.Entity}
     */
    public static Location getMiddleLocationOfPlayer(final Entity entity)
    {
        if (entity instanceof Player) {
            final Player player = (Player) entity;
            return entity.getLocation().clone().add(0, player.isSneaking() ?
                                                       (SNEAKING_PLAYER.getHeight() / 2) :
                                                       (PLAYER.getHeight() / 2), 0);
        }

        return entity.getLocation().clone().add(0, entity.getHeight() / 2, 0);
    }

    public double getHeight()
    {
        return height;
    }

    public double getOffset()
    {
        return offset;
    }
}

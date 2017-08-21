package de.photon.AACAdditionPro.util.entities.movement;


import de.photon.AACAdditionPro.util.mathematics.AxisAlignedBB;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.reflection.ReflectionUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import java.util.List;

public final class Collision
{
    /**
     * Looks for collisions that could occur during the movement and adds them to the initial {@link Location} to get an uncollided {@link Location}
     *
     * @param dependantEntity the Entity used for the reflection process
     * @param input           the initial {@link Location}
     * @param hitbox          the {@link Hitbox} of the used {@link org.bukkit.entity.Entity}
     *
     * @return the nearest, uncollided {@link Location}
     */
    public static Location getNearestUncollidedLocation(Entity dependantEntity, Location input, Hitbox hitbox)
    {
        return getNearestUncollidedLocation(dependantEntity, input, hitbox, new Vector());
    }

    /**
     * Looks for collisions that could occur during the movement and adds them to the initial {@link Location} to get an uncollided {@link Location}
     *
     * @param dependantEntity the Entity used for the reflection process
     * @param input           the initial {@link Location}
     * @param hitbox          the {@link Hitbox} of the used {@link org.bukkit.entity.Entity}
     * @param velocity        the planned movement
     *
     * @return the nearest, uncollided {@link Location}
     */
    public static Location getNearestUncollidedLocation(Entity dependantEntity, Location input, Hitbox hitbox, Vector velocity)
    {
        // Construct the BoundingBox
        AxisAlignedBB bb = hitbox.constructBoundingBox(input);
        // Add the scheduled movement
        bb.addCoordinates(velocity.getX(), velocity.getY(), velocity.getZ());

        // Get the collisions
        final List<AxisAlignedBB> collisions = ReflectionUtils.getCollisionBoxes(dependantEntity, bb);

        // Check if we would hit a y border block
        for (AxisAlignedBB collisionBox : collisions) {
            velocity.setY(collisionBox.calculateYOffset(bb, velocity.getY()));
        }

        bb.offset(0, velocity.getY(), 0);

        // Check if we would hit a x border block
        for (AxisAlignedBB collisionBox : collisions) {
            velocity.setX(collisionBox.calculateXOffset(bb, velocity.getX()));
        }

        bb.offset(velocity.getX(), 0, 0);

        // Check if we would hit a z border block
        for (AxisAlignedBB collisionBox : collisions) {
            velocity.setZ(collisionBox.calculateZOffset(bb, velocity.getZ()));
        }

        // No offset here as the bb is not used anymore afterwards.

        // Returns the cloned input with the needed offset.
        return input.clone().add(velocity);
    }
}

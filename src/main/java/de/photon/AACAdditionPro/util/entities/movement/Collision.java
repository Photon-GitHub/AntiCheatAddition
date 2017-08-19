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
     * @param coords          the planned movement
     *
     * @return the nearest, uncollided {@link Location}
     */
    public static Location getNearestUncollidedLocation(Entity dependantEntity, Location input, Hitbox hitbox, Vector coords)
    {
        // Construct the BoundingBox
        AxisAlignedBB bb = hitbox.constructBoundingBox(input);
        // Add the scheduled movement
        bb.addCoordinates(coords.getX(), coords.getY(), coords.getZ());

        // Get the collisions
        List<AxisAlignedBB> collisions = ReflectionUtils.getCollisionBoxes(dependantEntity, bb);

        // Check if we would hit a y border block
        for (AxisAlignedBB axisAlignedBB : collisions) {
            coords.setY(axisAlignedBB.calculateYOffset(bb, coords.getY()));
        }

        bb.offset(0, coords.getY(), 0);

        // Check if we would hit a x border block
        for (AxisAlignedBB axisAlignedBB : collisions) {
            coords.setX(axisAlignedBB.calculateYOffset(bb, coords.getX()));
        }

        bb.offset(coords.getX(), 0, 0);

        // Check if we would hit a z border block
        for (AxisAlignedBB axisAlignedBB : collisions) {
            coords.setZ(axisAlignedBB.calculateYOffset(bb, coords.getZ()));
        }

        bb.offset(0, 0, coords.getZ());

        // Returns the cloned input with the needed offset.
        return input.clone().add(coords);
    }
}

package de.photon.AACAdditionPro.util.fakeentity.movement;


import de.photon.AACAdditionPro.util.mathematics.AxisAlignedBB;
import de.photon.AACAdditionPro.util.mathematics.Hitbox;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.reflection.ReflectionUtils;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.util.BlockIterator;
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
     * @param velocity        the planned movement
     *
     * @return the vector to add to the input for the nearest, uncollided {@link Location}
     */
    public static Vector getNearestUncollidedLocation(Entity dependantEntity, Location input, Hitbox hitbox, Vector velocity)
    {
        // Do not touch the real velocity of the entity.
        velocity = velocity.clone();

        // Construct the BoundingBox
        final AxisAlignedBB bb = hitbox.constructBoundingBox(input);

        // Get the collisions
        final List<AxisAlignedBB> collisions = ReflectionUtils.getCollisionBoxes(dependantEntity, bb
                // Add the scheduled movement. This DOES NOT MODIFY INTERNAL VALUES, only call this for the Reflection!!!
                .addCoordinatesToNewBox(velocity.getX(), velocity.getY(), velocity.getZ()));

        // Check if we would hit a y border block
        for (AxisAlignedBB collisionBox : collisions)
        {
            velocity.setY(collisionBox.calculateYOffset(bb, velocity.getY()));
        }

        bb.offset(0, velocity.getY(), 0);

        // Check if we would hit a x border block
        for (AxisAlignedBB collisionBox : collisions)
        {
            velocity.setX(collisionBox.calculateXOffset(bb, velocity.getX()));
        }

        bb.offset(velocity.getX(), 0, 0);

        // Check if we would hit a z border block
        for (AxisAlignedBB collisionBox : collisions)
        {
            velocity.setZ(collisionBox.calculateZOffset(bb, velocity.getZ()));
        }

        // No offset here as the bb is not used anymore afterwards.

        // Returns the cloned input with the needed offset.
        return velocity;
    }

    /**
     * This method finds the next free space to a {@link Location} if only searching on the y - Axis.
     *
     * @return the {@link Location} of the closest free space found or a {@link Location} of y = 260 if no free space was found.
     */
    public static Location getClosestFreeSpaceYAxis(final Location location, final Hitbox hitbox)
    {
        // Short as no hitbox is larger than 32k blocks.
        // Represents the needed empty blocks, slabs (or other non-full blocks) are not included
        final short neededHeight = (short) Math.ceil(hitbox.getHeight());

        // The offset of the next free space to the location.
        double minDeltaY = Double.MAX_VALUE;

        // Set to 260 as that is the default value if nothing else is found.
        double currentY = 260;

        final BlockIterator blockIterator = new BlockIterator(location.getWorld(),
                                                              location.toVector(),
                                                              // From the sky to the void to have less needed calculations
                                                              new Vector(0, -1, 0),
                                                              // Add 20 to check both over and below the starting location.
                                                              20,
                                                              (int) Math.min(
                                                                      // Make sure the BlockIterator will not iterate into the void.
                                                                      location.getY() + 20,
                                                                      // 40 as default length.
                                                                      40));

        short currentHeight = 0;
        Block currentBlock;

        while (blockIterator.hasNext())
        {
            currentBlock = blockIterator.next();

            final double originOffset = MathUtils.offset(location.getY(), currentBlock.getY());

            // >= To prefer "higher" positions and improve performance.
            if (originOffset >= minDeltaY)
            {
                // Now we can only get worse results.
                break;
            }

            // Check if the block is empty
            if (currentBlock.isEmpty())
            {
                // If the empty space is big enough
                if (++currentHeight >= neededHeight)
                {
                    minDeltaY = originOffset;
                    currentY = currentBlock.getY();
                }
            }
            else
            {
                currentHeight = 0;
            }
        }

        final Location spawnLocation = location.clone();
        spawnLocation.setY(currentY);
        return spawnLocation;
    }
}

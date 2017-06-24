package de.photon.AACAdditionPro.util.mathematics;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public final class VectorUtils
{
    /**
     * Get to know where the {@link Vector} intersects with a {@link org.bukkit.block.Block}.
     * Non-Solid {@link org.bukkit.block.Block}s are ignored.
     *
     * @param start the starting {@link Location}
     * @param a     the {@link Vector} which should be checked
     *
     * @return The length when the {@link Vector} intersects or 0 if no intersection was found
     */
    public static double getFirstVectorIntersectionWithBlock(final Location start, final Vector a)
    {
        final int length = (int) Math.floor(a.length());
        if (length >= 1) {
            try {
                final BlockIterator blockIterator = new BlockIterator(start.getWorld(), start.toVector(), a, 0, length);
                while (blockIterator.hasNext()) {
                    final Block block = blockIterator.next();
                    if (block.getType().isOccluding() && block.getType().isSolid()) {
                        return block.getLocation().distance(start);
                    }
                }
            } catch (final IllegalStateException ignored) {
            }
        }
        return 0;
    }

    /**
     * Checks if the {@link Vector} a intersects with an occluding and solid {@link Block} after length.
     *
     * @param start  the {@link Location} where the {@link Vector} a is starting
     * @param a      the {@link Vector} which should be checked
     * @param length the {@link Block}-check takes place at the location start + a.normalize().multiply(length)
     */
    public static boolean vectorIntersectsWithBlockAt(final Location start, final Vector a, final double length)
    {
        final Material type = start.clone().add(a.clone().normalize().multiply(length)).getBlock().getType();
        return type.isOccluding() && type.isSolid();
    }

// --Commented out by Inspection START (08.06.17 20:16):
//    public static boolean getTargetedPlayerOfPlayer(final Player player, final Player target, final double maxRange, final double precision)
//    {
//        Validate.notNull(player, "player may not be null");
//        Validate.isTrue(maxRange > 0D, "the maximum range has to be greater than 0");
//        Validate.isTrue(precision > 0D && precision < 1D, "the precision has to be greater than 0 and smaller than 1");
//
//        // Target cannot be hit when it is null
//        if (target == null ||
//            // Target cannot be hit when it is in another world
//            !player.getWorld().getName().equals(target.getWorld().getName()))
//        {
//            return false;
//        }
//
//        // Player and target have the same name.
//        if(player.getName().equals(target.getName()))
//        {
//            return true;
//        }
//
//        final double squaredMaxRange = maxRange * maxRange;
//
//        final Location fromLocation = player.getEyeLocation();
//        final Vector playerDirection = fromLocation.getDirection().normalize();
//        final Vector playerVectorPos = fromLocation.toVector();
//
//        /**Displays all the {@link Location}s of the target that are checked:
//         * [0] == the maximum {@link Location} ({@link Location} of target with full height)
//         * [1] == the middle {@link Location} ({@link Location} of target with halved height)
//         * [2] == the normal {@link Location} (foot {@link Location})*/
//        final Location[] targetLocations = new Location[]{
//                Hitbox.getLocationWithHeight(target),
//                Hitbox.getMiddleLocationOfPlayer(target),
//                target.getLocation()
//        };
//
//        // Check the angle to the target:
//        for (final Location check_location : targetLocations) {
//            final double target_distance = check_location.distanceSquared(fromLocation);
//
//            if (target_distance <= squaredMaxRange) {
//                final Vector toTarget = check_location.toVector().subtract(playerVectorPos).normalize();
//                // check the dotProduct instead of the angle, because it's faster:
//                final double dotProduct = toTarget.dot(playerDirection);
//
//                if (dotProduct > precision &&
//                    player.hasLineOfSight(target) &&
//                    target_distance < Double.MAX_VALUE)
//                {
//                    return true;
//                }
//            }
//        }
//
//        return false;
//    }
// --Commented out by Inspection STOP (08.06.17 20:16)
}
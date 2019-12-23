package de.photon.aacadditionpro.util.reflection;

import de.photon.aacadditionpro.util.mathematics.AxisAlignedBB;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.util.List;

public final class ReflectionUtils
{
    private ReflectionUtils() {}

    private static final String versionNumber = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];

    /**
     * Used to get the version {@link String} that is necessary for net.minecraft.server reflection
     *
     * @return e.g. v1_11_R1
     */
    public static String getVersionString()
    {
        return versionNumber;
    }

    public static AxisAlignedBB[] getCollisionBoxes(Entity entity, AxisAlignedBB boundingBox)
    {
        // First we need a NMS bounding box
        final Object nmsAxisAlignedBB = Reflect
                .fromNMS("AxisAlignedBB")
                .constructor(double.class, double.class, double.class, double.class, double.class, double.class)
                .instance(boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ(),
                          boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ());

        // Now we need the NMS entity of the player (since the bot has none)
        final Object nmsHandle = Reflect
                .from(entity.getClass())
                .method("getHandle")
                .invoke(entity);

        // Now we need to call getCubes(Entity, AxisAlignedBB) on the world
        final Object nmsWorld = Reflect
                .fromOBC("CraftWorld")
                .field("world")
                .from(entity.getWorld())
                .as(Object.class);

        // Get all the cubes
        final List boxList = (List) Reflect
                .fromNMS("World")
                .method("getCubes")
                .invoke(nmsWorld, nmsHandle, nmsAxisAlignedBB);

        // Transfer them to an AxisAlignedBBs array.
        final AxisAlignedBB[] boxes = new AxisAlignedBB[boxList.size()];
        int index = 0;

        for (Object nmsAABB : boxList) {
            // nmsAABB is a NMS AxisAlignedBB
            boxes[index++] = (AxisAlignedBB.fromNms(nmsAABB));
        }
        return boxes;
    }

}

package de.photon.AACAdditionPro.util.multiversion;

import de.photon.AACAdditionPro.util.mathematics.AxisAlignedBB;
import de.photon.AACAdditionPro.util.reflection.Reflect;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class ReflectionUtils
{
    private static final String versionNumber;

    static {
        versionNumber = Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
    }

    /**
     * Used to get the version {@link String} that is necessary for net.minecraft.server reflection
     *
     * @return e.g. v1_11_R1
     */
    public static String getVersionString()
    {
        return versionNumber;
    }

    public static List<AxisAlignedBB> getCollisionBoxes(Player player, AxisAlignedBB boundingBox)
    {
        // First we need a NMS bounding box
        Object nmsAxisAlignedBB = Reflect
                .from("net.minecraft.server." + getVersionString() + ".AxisAlignedBB")
                .constructor(double.class, double.class, double.class, double.class, double.class, double.class)
                .instance(boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ(),
                        boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ() );

        // Now we need the NMS entity of the player (since the bot has none)
        Object nmsHandle = Reflect
                .from("org.bukkit.craftbukkit." + ReflectionUtils.getVersionString() + ".entity.CraftPlayer")
                .method("getHandle")
                .invoke(player);

        // Now we need to call getCubes(Entity, AxisAlignedBB) on the world
        Object nmsWorld = Reflect
                .from("org.bukkit.craftbukkit." + getVersionString() + ".CraftWorld")
                .field("world")
                .from(player.getWorld())
                .as(Object.class);

        Object returnVal = Reflect
                .from("net.minecraft.server." + getVersionString() + ".World")
                .method("getCubes")
                .invoke(nmsWorld, nmsHandle, nmsAxisAlignedBB);

        // Now lets see what we got
        List<AxisAlignedBB> boxes = new ArrayList<>();
        List list = (List) returnVal;
        for ( Object o : list ) {
            // o is a NMS AxisAlignedBB
            double minX = Reflect.from(o.getClass()).field(0).from(o).asDouble();
            double minY = Reflect.from(o.getClass()).field(1).from(o).asDouble();
            double minZ = Reflect.from(o.getClass()).field(2).from(o).asDouble();
            double maxX = Reflect.from(o.getClass()).field(3).from(o).asDouble();
            double maxY = Reflect.from(o.getClass()).field(4).from(o).asDouble();
            double maxZ = Reflect.from(o.getClass()).field(5).from(o).asDouble();

            AxisAlignedBB box = new AxisAlignedBB( minX, minY, minZ, maxX, maxY, maxZ );
            boxes.add( box );
        }

        return boxes;
    }

}

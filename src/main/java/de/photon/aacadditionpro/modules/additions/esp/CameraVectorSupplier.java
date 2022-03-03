package de.photon.aacadditionpro.modules.additions.esp;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.util.minecraft.world.MaterialUtil;
import lombok.val;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.bukkit.util.Vector;

public interface CameraVectorSupplier
{
    CameraVectorSupplier INSTANCE = AACAdditionPro.getInstance().getConfig().getBoolean("Esp.calculate_third_person_modes", false) ? new ThirdPersonCameraSupplier() : new SingleCameraSupplier();

    /**
     * Get to know where the {@link Vector} intersects with a {@link org.bukkit.block.Block}.
     * Non-Occluding {@link Block}s as defined in {@link MaterialUtil#isReallyOccluding(Material)} are ignored.
     *
     * @param start     the starting {@link Location}
     * @param direction the {@link Vector} which should be checked
     *
     * @return The length when the {@link Vector} intersects or 0 if no intersection was found
     */
    static double getDistanceToFirstIntersectionWithBlock(final Location start, final Vector direction)
    {
        Preconditions.checkNotNull(start.getWorld(), "RayTrace: Unknown start world.");
        val length = (int) direction.length();

        if (length >= 1) {
            try {
                val blockIterator = new BlockIterator(start.getWorld(), start.toVector(), direction, 0, length);
                Block block;
                while (blockIterator.hasNext()) {
                    block = blockIterator.next();
                    // Account for a Spigot bug: BARRIER and MOB_SPAWNER are not occluding blocks
                    // Use the middle location of the Block instead of the simple location.
                    if (MaterialUtil.isReallyOccluding(block.getType())) return block.getLocation().clone().add(0.5, 0.5, 0.5).distance(start);
                }
            } catch (IllegalStateException exception) {
                // Just in case the start block could not be found for some reason or a chunk is loaded async.
                return 0;
            }
        }
        return 0;
    }

    Location[] getCameraLocations(Player player);
}

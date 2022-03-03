package de.photon.aacadditionpro.modules.additions.esp;

import de.photon.aacadditionpro.util.mathematics.Hitbox;
import de.photon.aacadditionpro.util.mathematics.ResetLocation;
import de.photon.aacadditionpro.util.mathematics.ResetVector;
import de.photon.aacadditionpro.util.minecraft.world.InternalPotion;
import de.photon.aacadditionpro.util.minecraft.world.MaterialUtil;
import de.photon.aacadditionpro.util.minecraft.world.WorldUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
class CanSee
{
    // The real MAX_FOV is 110 (quake pro), which results in 150° according to tests.
    // 150° + 15° (compensation) = 165°
    public static final double MAX_FOV = Math.toRadians(165D);

    public static boolean canSee(Player observer, Player watched)
    {
        // Glowing.
        if (InternalPotion.GLOWING.hasPotionEffect(watched)) return true;

        // ----------------------------------- Calculation ---------------------------------- //
        final Location[] cameraLocations = CameraVectorSupplier.INSTANCE.getCameraLocations(observer);
        final Location[] watchedHitboxLocations = Hitbox.fromPlayer(watched).getEspLocations(watched.getLocation());
        final Vector viewDirection = observer.getLocation().getDirection();

        for (Location cameraLocation : cameraLocations) {
            ResetVector between = new ResetVector(cameraLocation.toVector());
            for (Location hitLoc : watchedHitboxLocations) {
                // Effectively hitLoc - cameraLocation without a clone.
                between.resetToBase().multiply(-1).add(hitLoc.toVector());

                // Ignore directions that cannot be seen by the player due to FOV.
                if (viewDirection.angle(between) > MAX_FOV) continue;

                // Make sure the chunks are loaded.
                // If the chunks are not loaded assume the players can see each other.
                if (!WorldUtil.INSTANCE.areChunksLoadedBetweenLocations(cameraLocation, hitLoc)) return true;

                // No intersection found
                if (canSeeHeuristic(cameraLocation, between, hitLoc)) return true;
            }
        }
        return false;
    }

    public static boolean canSeeHeuristic(Location from, Vector between, Location to)
    {
        final ResetLocation resetFrom = new ResetLocation(from);
        final ResetVector normalBetween = new ResetVector(between.normalize());
        Block block;
        for (double d : heuristicScalars(from.distance(to))) {
            block = resetFrom.add(normalBetween.multiply(d)).getBlock();
            // An occluding block exists.
            //noinspection ConstantConditions
            if (block != null && !block.isEmpty() && MaterialUtil.isReallyOccluding(block.getType())) return false;

            resetFrom.resetToBase();
            normalBetween.resetToBase();
        }
        return true;
    }

    private static double[] heuristicScalars(double distance)
    {
        if (distance <= 2) return new double[]{1};
        if (distance <= 3) return new double[]{1, distance - 1};
        if (distance <= 5) return new double[]{1, 1.5, 2, distance - 2, distance - 1.5, distance - 1};
        if (distance <= 10) return new double[]{1, 1.5, 2, 2.5, distance / 2, distance - 2.5, distance - 2, distance - 1.5, distance - 1};
        if (distance <= 25) return new double[]{1, 1.5, 2, 2.5, distance / 4, distance / 2, 3 * distance / 4, distance - 2.5, distance - 2, distance - 1.5, distance - 1};
        return new double[]{1, 1.5, 2, 2.5, distance / 4, distance / 3, distance / 2, 2 * distance / 3, 3 * distance / 4, distance - 2.5, distance - 2, distance - 1.5, distance - 1};
    }
}

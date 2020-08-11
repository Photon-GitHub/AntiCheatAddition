package de.photon.aacadditionpro.modules.checks.esp;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.mathematics.Hitbox;
import de.photon.aacadditionpro.util.potion.InternalPotionEffectType;
import de.photon.aacadditionpro.util.potion.PotionUtil;
import de.photon.aacadditionpro.util.world.BlockUtils;
import de.photon.aacadditionpro.util.world.ChunkUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CanSee
{
    // The real MAX_FOV is 110 (quake pro), which results in 150° according to tests.
    // 150° + 15° (compensation) = 165°
    protected static final double MAX_FOV = Math.toRadians(165D);

    /**
     * This should supply all necessary camera vectors.
     */
    private static final Function<Player, Vector[]> CAMERA_VECTOR_SUPPLIER;
    private static final boolean LOW_VECTOR_HITBOXES;

    static {
        if (AACAdditionPro.getInstance().getConfig().getBoolean(ModuleType.ESP.getConfigString() + ".calculate_third_person_modes", true)) {
            CAMERA_VECTOR_SUPPLIER = new CanSeeThirdPerson();
        } else {
            CAMERA_VECTOR_SUPPLIER = new CanSeeNoThirdPerson();
        }
        LOW_VECTOR_HITBOXES = AACAdditionPro.getInstance().getConfig().getBoolean(ModuleType.ESP.getConfigString() + ".low_vector_hitboxes", true);
    }

    /**
     * Determines whether a {@link User} can see another {@link User}
     */
    public static boolean canSee(Player observer, Player watched)
    {
        // Glowing.
        if (PotionUtil.hasPotionEffect(watched, InternalPotionEffectType.GLOWING)) {
            return true;
        }

        // ----------------------------------- Calculation ---------------------------------- //

        final Vector[] cameraVectors = CAMERA_VECTOR_SUPPLIER.apply(observer);

        // Get the Vectors of the hitbox to check.
        final Hitbox hitbox = watched.isSneaking() ? Hitbox.ESP_SNEAKING_PLAYER : Hitbox.ESP_PLAYER;
        final Vector[] watchedHitboxVectors = LOW_VECTOR_HITBOXES ?
                                              hitbox.getLowResolutionCalculationVectors(watched.getLocation()) :
                                              hitbox.getCalculationVectors(watched.getLocation());

        // The distance of the intersections in the same block is equal as of the BlockIterator mechanics.
        // Use ArrayList because we do not cache many values in here and therefore HashSet is more expensive.
        final double[] lastIntersectionsCache = new double[10];
        int cacheIndex = 0;

        for (Vector cameraVector : cameraVectors) {
            for (final Vector destinationVector : watchedHitboxVectors) {
                final Location start = cameraVector.toLocation(observer.getWorld());
                // The resulting Vector
                // The camera is not blocked by non-solid blocks
                // Vector is intersecting with some blocks
                //
                // Cloning IS needed as we are in a second loop.
                final Vector between = destinationVector.clone().subtract(cameraVector);

                // ---------------------------------------------- FOV ----------------------------------------------- //
                final Vector cameraRotation = cameraVector.clone().subtract(observer.getLocation().toVector());

                if (cameraRotation.angle(between) > MAX_FOV) {
                    continue;
                }

                // ---------------------------------------- Cache Calculation --------------------------------------- //

                // Make sure the chunks are loaded.
                if (!ChunkUtils.areChunksLoadedBetweenLocations(start, start.clone().add(between))) {
                    // If the chunks are not loaded assume the players can see each other.
                    return true;
                }

                // Check if there is a block on a cached location that obstructs the view.
                if (checkCache(lastIntersectionsCache, start, between)) {
                    continue;
                }

                // --------------------------------------- Normal Calculation --------------------------------------- //

                final double intersect = EspUtil.getDistanceToFirstIntersectionWithBlock(start, between);

                // No intersection found
                if (intersect == 0) {
                    return true;
                }

                lastIntersectionsCache[cacheIndex] = intersect;
                cacheIndex = (cacheIndex + 1) % lastIntersectionsCache.length;
            }

            // Low probability to help after the camera view was changed. -> clearing
            Arrays.fill(lastIntersectionsCache, 0);
        }

        return false;
    }

    private static boolean checkCache(double[] lastIntersectionsCache, Location start, Vector between)
    {
        // Use the tempBetween vector to avoid unnecessary cloning.
        final Vector tempBetween = between.clone();

        Location cacheLocation;
        for (int i = 0; i < lastIntersectionsCache.length; ++i) {
            // Not yet cached.
            if (lastIntersectionsCache[i] != 0) {
                cacheLocation = start.add(tempBetween.normalize().multiply(lastIntersectionsCache[i]));

                final Material type = cacheLocation.getBlock().getType();
                if (BlockUtils.isReallyOccluding(type) && type.isSolid()) {
                    return true;
                }

                // Reset the tempBetween vector
                tempBetween.setX(between.getX());
                tempBetween.setY(between.getY());
                tempBetween.setZ(between.getZ());
            }
        }
        return false;
    }
}

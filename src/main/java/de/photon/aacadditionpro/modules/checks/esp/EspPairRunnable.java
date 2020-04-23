package de.photon.aacadditionpro.modules.checks.esp;

import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.mathematics.Hitbox;
import de.photon.aacadditionpro.util.mathematics.VectorUtils;
import de.photon.aacadditionpro.util.visibility.HideMode;
import de.photon.aacadditionpro.util.world.BlockUtils;
import de.photon.aacadditionpro.util.world.ChunkUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public class EspPairRunnable implements Runnable
{
    // The real MAX_FOV is 110 (quake pro), which results in 150° according to tests.
    // 150° + 15° (compensation) = 165°
    private static final double MAX_FOV = Math.toRadians(165D);

    private final User observingUser;
    private final User watched;

    private final int playerTrackingRange;
    private final boolean hideAfterRenderDistance;

    /**
     * Determines if two {@link User}s can see each other.
     */
    private static boolean canSee(final User observerUser, final User watchedUser)
    {
        final Player observer = observerUser.getPlayer();
        final Player watched = watchedUser.getPlayer();

        // Not bypassed
        if (observerUser.isBypassed(ModuleType.ESP) ||
            // Has not logged in recently to prevent bugs
            observerUser.hasLoggedInRecently(3000) ||
            // Glowing handling
            // Glowing does not exist in 1.8.8
            (ServerVersion.getActiveServerVersion() != ServerVersion.MC188 &&
             // If an entity is glowing it can always be seen.
             watched.hasPotionEffect(PotionEffectType.GLOWING)))
        {
            return true;
        }

        // ----------------------------------- Calculation ---------------------------------- //

        final Vector[] cameraVectors = VectorUtils.getCameraVectors(observer);

        // Get the Vectors of the hitbox to check.
        final Vector[] watchedHitboxVectors = (watched.isSneaking() ?
                                               Hitbox.ESP_SNEAKING_PLAYER :
                                               Hitbox.ESP_PLAYER).getCalculationVectors(watched.getLocation(), true);

        // The distance of the intersections in the same block is equal as of the
        // BlockIterator mechanics.
        final Set<Double> lastIntersectionsCache = new HashSet<>();

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

                boolean cacheHit = false;

                Location cacheLocation;
                for (Double length : lastIntersectionsCache) {
                    cacheLocation = start.clone().add(between.clone().normalize().multiply(length));

                    // Not yet cached.
                    if (length == 0) {
                        continue;
                    }

                    final Material type = cacheLocation.getBlock().getType();

                    if (BlockUtils.isReallyOccluding(type) && type.isSolid()) {
                        cacheHit = true;
                        break;
                    }
                }

                if (cacheHit) {
                    continue;
                }

                // --------------------------------------- Normal Calculation --------------------------------------- //

                final double intersect = VectorUtils.getDistanceToFirstIntersectionWithBlock(start, between);

                // No intersection found
                if (intersect == 0) {
                    return true;
                }

                lastIntersectionsCache.add(intersect);
            }
        }

        // Low probability to help after the camera view was changed. -> clearing
        lastIntersectionsCache.clear();
        return false;
    }

    @Override
    public void run()
    {
        // The users are always in the same world (see above)
        final double pairDistanceSquared = observingUser.getPlayer().getLocation().distanceSquared(watched.getPlayer().getLocation());

        // Less than 1 block distance
        // Everything (smaller than 1)^2 will result in something smaller than 1
        if (pairDistanceSquared < 1) {
            Esp.updatePairHideMode(observingUser, watched, HideMode.NONE);
            return;
        }

        if (pairDistanceSquared > this.playerTrackingRange) {
            Esp.updatePairHideMode(observingUser, watched, hideAfterRenderDistance ?
                                                           HideMode.FULL :
                                                           HideMode.NONE);
            return;
        }

        // Update hide mode in both directions.
        Esp.updateHideMode(observingUser, watched.getPlayer(),
                           canSee(observingUser, watched) ?
                           // Is the user visible
                           HideMode.NONE :
                           // If the observed player is sneaking hide him fully
                           (watched.getPlayer().isSneaking() ?
                            HideMode.FULL :
                            HideMode.INFORMATION_ONLY));

        Esp.updateHideMode(watched, observingUser.getPlayer(),
                           canSee(watched, observingUser) ?
                           // Is the user visible
                           HideMode.NONE :
                           // If the observed player is sneaking hide him fully
                           (observingUser.getPlayer().isSneaking() ?
                            HideMode.FULL :
                            HideMode.INFORMATION_ONLY));
    }
}

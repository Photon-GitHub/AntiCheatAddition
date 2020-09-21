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
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CanSee
{
    // The real MAX_FOV is 110 (quake pro), which results in 150° according to tests.
    // 150° + 15° (compensation) = 165°
    public static final double MAX_FOV = Math.toRadians(165D);

    /**
     * This should supply all necessary camera vectors.
     */
    private static final Function<Player, Vector[]> CAMERA_VECTOR_SUPPLIER;
    private static final boolean LOW_VECTOR_HITBOXES;

    static {
        CAMERA_VECTOR_SUPPLIER = AACAdditionPro.getInstance().getConfig().getBoolean(ModuleType.ESP.getConfigString() + ".calculate_third_person_modes", true) ?
                                 new CanSeeThirdPerson() :
                                 new CanSeeNoThirdPerson();

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

        // The needed variables for the calculation.
        // Use ResetLocation to reduce the amount of object creations to a minimum.
        final ResetLocation cameraLocation = new ResetLocation();
        // Another ResetLocation for a computation to reduce the amount of clone() operation.
        final ResetLocation cameraLocationPlusBetween = new ResetLocation();
        final ResetVector observerLocationVector = new ResetVector(observer.getLocation().toVector());

        Vector between;
        double intersect;
        for (final Vector cameraVector : cameraVectors) {
            cameraLocation.setBaseLocation(cameraVector.toLocation(observer.getWorld()));
            // No cloning of baseLocation necessary here as the baseLocation is never changed.
            cameraLocationPlusBetween.setBaseLocation(cameraLocation.baseLocation);

            for (final Vector destinationVector : watchedHitboxVectors) {
                cameraLocation.resetToBase();

                // The resulting Vector
                // The camera is not blocked by non-solid blocks
                // Vector is intersecting with some blocks
                //
                // Cloning IS needed as we are in a second loop.
                between = destinationVector.clone().subtract(cameraVector);

                // ---------------------------------------------- FOV ----------------------------------------------- //

                // Subtract the wrong way around and multiply with -1 to avoid .clone() operation.
                if (observerLocationVector.resetToBase().subtract(cameraVector).multiply(-1).angle(between) > MAX_FOV) {
                    continue;
                }

                // ---------------------------------------- Cache Calculation --------------------------------------- //

                // Make sure the chunks are loaded.
                if (!ChunkUtils.areChunksLoadedBetweenLocations(cameraLocation, cameraLocationPlusBetween.resetToBase().add(between))) {
                    // If the chunks are not loaded assume the players can see each other.
                    return true;
                }

                // Check if there is a block on a cached location that obstructs the view.
                if (checkCache(lastIntersectionsCache, cameraLocation, between)) {
                    continue;
                }

                // --------------------------------------- Normal Calculation --------------------------------------- //

                intersect = EspUtil.getDistanceToFirstIntersectionWithBlock(cameraLocation, between);

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
        // Use the tempBetween vector and tempStart location to avoid unnecessary cloning.
        final ResetVector resetBetween = new ResetVector(between);
        final ResetLocation resetStart = new ResetLocation(start);

        Location cacheLocation;
        // The lastIntersectionsCache[i] != 0 condition ensures that the cache is filled.
        // If a 0 is encountered we can assume that the cache just filled until that point, as it is filled from
        // beginning to end.
        for (int i = 0; i < lastIntersectionsCache.length && lastIntersectionsCache[i] != 0; ++i) {
            cacheLocation = resetStart.add(resetBetween.normalize().multiply(lastIntersectionsCache[i]));

            final Material type = cacheLocation.getBlock().getType();
            if (BlockUtils.isReallyOccluding(type) && type.isSolid()) {
                return true;
            }

            // Reset the vector and location
            resetBetween.resetToBase();
            resetStart.resetToBase();
        }
        return false;
    }

    private static class ResetLocation extends Location
    {
        @Setter
        private Location baseLocation;

        public ResetLocation()
        {
            super(null, 0, 0, 0);
        }

        public ResetLocation(Location baseLocation)
        {
            super(baseLocation.getWorld(), baseLocation.getX(), baseLocation.getY(), baseLocation.getZ());
            this.baseLocation = baseLocation;
        }

        private ResetLocation resetToBase()
        {
            this.setX(baseLocation.getX());
            this.setY(baseLocation.getY());
            this.setZ(baseLocation.getZ());
            return this;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            return Objects.equals(baseLocation, ((ResetLocation) o).baseLocation);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(super.hashCode(), baseLocation);
        }
    }

    private static class ResetVector extends Vector
    {
        private final Vector baseVector;

        public ResetVector(Vector baseVector)
        {
            this.baseVector = baseVector;
            resetToBase();
        }

        private ResetVector resetToBase()
        {
            this.x = baseVector.getX();
            this.y = baseVector.getY();
            this.z = baseVector.getZ();
            return this;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            if (!super.equals(o)) return false;
            return Objects.equals(baseVector, ((ResetVector) o).baseVector);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(super.hashCode(), baseVector);
        }
    }
}

package de.photon.anticheataddition.util.minecraft.world.region;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import lombok.experimental.UtilityClass;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Set;

/**
 * Utility class for interacting with WorldGuard to fetch specific types of regions from multiple worlds.
 * This class leverages the WorldGuard API to filter and translate regions based on specific flags.
 * Also, this class is used to isolate the WorldGuard API imports from the rest of the codebase to allow not using WorldGuard.
 */
@UtilityClass
public class WorldGuardRegionUtil
{
    /**
     * Loads regions from the specified worlds where PvP is not allowed.
     *
     * @param enabledWorlds the set of worlds to check for PvP-disabled regions.
     *
     * @return a set of {@link Region} objects representing areas where PvP is disabled.
     */
    public static Set<Region> loadNoPVPRegions(Set<World> enabledWorlds)
    {
        return loadRegions(enabledWorlds, Set.of(), Set.of(Flags.PVP));
    }

    /**
     * General method to load regions based on allowed and denied flags.
     *
     * @param worlds     the set of worlds to load regions from.
     * @param allowFlags the set of flags that must be allowed for a region to be included.
     * @param denyFlags  the set of flags that must not be denied for a region to be included.
     *
     * @return a set of {@link Region} objects that meet the specified criteria.
     */
    public static Set<Region> loadRegions(Set<World> worlds, Set<Flag<?>> allowFlags, Set<Flag<?>> denyFlags)
    {
        final Set<Region> regions = new HashSet<>();
        final var regionContainer = WorldGuard.getInstance().getPlatform().getRegionContainer();

        for (World world : worlds) {
            final var regionManager = regionContainer.get(BukkitAdapter.adapt(world));
            if (regionManager == null) continue;

            for (var region : regionManager.getRegions().values()) {
                if (allowFlags.stream().map(region::getFlag).anyMatch(regionFlag -> regionFlag == StateFlag.State.DENY)) continue;
                if (denyFlags.stream().map(region::getFlag).anyMatch(regionFlag -> regionFlag == StateFlag.State.ALLOW)) continue;

                regions.add(toACARegion(world, region));
            }
        }
        return Set.copyOf(regions);
    }

    /**
     * Converts a WorldGuard {@link ProtectedRegion} into a {@link Region} object specific to this application.
     *
     * @param world  the world in which the region exists.
     * @param region the WorldGuard region to convert.
     *
     * @return a {@link Region} object with coordinates adapted to AntiCheatAddition's needs.
     */
    public static Region toACARegion(World world, ProtectedRegion region)
    {
        final var min = region.getMinimumPoint();
        final var max = region.getMaximumPoint();
        return new Region(world, min.x(), min.z(), max.x(), max.z());
    }
}

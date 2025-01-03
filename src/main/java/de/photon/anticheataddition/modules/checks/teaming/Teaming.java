package de.photon.anticheataddition.modules.checks.teaming;

import com.github.davidmoten.rtreemulti.Entry;
import com.github.davidmoten.rtreemulti.RTree;
import com.github.davidmoten.rtreemulti.geometry.Point;
import com.google.common.base.Preconditions;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.mathematics.TimeUtil;
import de.photon.anticheataddition.util.minecraft.world.region.Region;
import de.photon.anticheataddition.util.minecraft.world.region.WorldGuardRegionUtil;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class Teaming extends ViolationModule implements Listener
{
    public static final Teaming INSTANCE = new Teaming();
    private static final long CHECK_INTERVAL = TimeUtil.toTicks(5000);

    private Teaming()
    {
        super("Teaming");
    }

    private Set<World> loadEnabledWorlds()
    {
        final Set<World> worlds = new HashSet<>();
        for (final String key : loadStringList(".enabled_worlds")) {
            final var world = Bukkit.getWorld(key);
            if (world == null) {
                Log.fine(() -> "Unable to load world \"" + key + "\" in teaming check.");
                continue;
            }
            worlds.add(world);
        }
        return Set.copyOf(worlds);
    }

    private Set<Region> loadSafeZones(Set<World> enabledWorlds)
    {
        final Set<Region> safeZones = new HashSet<>();
        for (final String s : loadStringList(".safe_zones")) {
            try {
                final var region = Region.parseRegion(s);
                if (enabledWorlds.contains(region.world())) safeZones.add(region);
            } catch (NullPointerException e) {
                Log.severe(() -> "Unable to load safe zone \"" + s + "\" in teaming check, is the world correct?");
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.severe(() -> "Unable to load safe zone \"" + s + "\" in teaming check, are all coordinates present?");
            }
        }
        return Set.copyOf(safeZones);
    }

    @Override
    public void enable()
    {
        final var enabledWorlds = loadEnabledWorlds();

        final var safeZonesLoading = new HashSet<>(loadSafeZones(enabledWorlds));
        if (loadBoolean(".worldguard", false)) safeZonesLoading.addAll(WorldGuardRegionUtil.loadNoPVPRegions(enabledWorlds));
        final var safeZones = Set.copyOf(safeZonesLoading);

        final double proximityRange = loadDouble(".proximity_range", 4.5);

        final int noPvpTime = loadInt(".no_pvp_time", 6000);

        final int allowedSize = loadInt(".allowed_size", 1);
        Preconditions.checkArgument(allowedSize > 0, "The Teaming allowed_size must be greater than 0.");

        Bukkit.getScheduler().runTaskTimer(AntiCheatAddition.getInstance(), () -> {

            for (World world : enabledWorlds) {
                // Pre-calculate the entries for the RTree for faster initialization.
                final List<Entry<Player, Point>> entries = new ArrayList<>();
                for (Player player : world.getPlayers()) {
                    final User user = User.getUser(player);
                    if (!User.isUserInvalid(user, Teaming.INSTANCE)
                        // Correct game modes.
                        && user.inAdventureOrSurvivalMode()
                        // Not engaged in pvp.
                        && user.getTimeMap().at(TimeKey.COMBAT).notRecentlyUpdated(noPvpTime)) {
                        final var loc = player.getLocation();
                        // Not in a bypassed region.
                        if (safeZones.stream().noneMatch(safeZone -> safeZone.isInsideRegion(loc))) {
                            entries.add(User.rTreeEntryFromPlayer(player));
                        }
                    }
                }

                // Create an RTree for the players in the world.
                RTree<Player, Point> rTree = RTree.dimensions(3).create(entries);
                final var origin = Point.create(0, 0, 0);

                while (!rTree.isEmpty()) {
                    final var firstNode = rTree.nearest(origin, Double.POSITIVE_INFINITY, 1).iterator().next();
                    final var teamNodes = rTree.nearest(firstNode.geometry(), proximityRange, 1000);

                    // Get the players.
                    final List<Player> team = new ArrayList<>();
                    for (Entry<Player, Point> node : teamNodes) {
                        // Players need to see each other to be considered to be in a team.
                        if (firstNode.value().canSee(node.value()) && node.value().canSee(firstNode.value())) team.add(node.value());
                    }

                    Log.finer(() -> "Teaming | Team: " + team.stream().map(Player::getName).collect(Collectors.joining(", ")));

                    rTree = rTree.delete(teamNodes);

                    // Team is too big
                    final int vl = team.size() - allowedSize;
                    if (vl <= 0) continue;

                    for (final var player : team) this.getManagement().flag(Flag.of(player).setAddedVl(vl));
                }
            }
        }, 1L, CHECK_INTERVAL);
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this)
                                       .loadThresholdsToManagement()
                                       .withDecay(300, 1)
                                       .build();
    }
}

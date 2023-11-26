package de.photon.anticheataddition.modules.checks.teaming;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.datastructure.kdtree.Entity3DTree;
import de.photon.anticheataddition.util.mathematics.TimeUtil;
import de.photon.anticheataddition.util.messaging.Log;
import de.photon.anticheataddition.util.minecraft.world.Region;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Teaming extends ViolationModule implements Listener
{
    public static final Teaming INSTANCE = new Teaming();
    private static final long CHECK_INTERVAL = TimeUtil.toTicks(5000);

    private Teaming()
    {
        super("Teaming");
    }

    private Set<Region> loadSafeZones()
    {
        final Set<Region> safeZones = new HashSet<>();
        for (final String s : loadStringList(".safe_zones")) {
            try {
                safeZones.add(Region.parseRegion(s));
            } catch (NullPointerException e) {
                Log.severe(() -> "Unable to load safe zone \"" + s + "\" in teaming check, is the world correct?");
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.severe(() -> "Unable to load safe zone \"" + s + "\" in teaming check, are all coordinates present?");
            }
        }
        return Set.copyOf(safeZones);
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

    @Override
    public void enable()
    {
        final var safeZones = loadSafeZones();
        final var enabledWorlds = loadEnabledWorlds();

        final double proximityRange = loadDouble(".proximity_range", 4.5);

        final int noPvpTime = loadInt(".no_pvp_time", 6000);

        final int allowedSize = loadInt(".allowed_size", 1);
        Preconditions.checkArgument(allowedSize > 0, "The Teaming allowed_size must be greater than 0.");

        Bukkit.getScheduler().scheduleSyncRepeatingTask(AntiCheatAddition.getInstance(), () -> {
            final Entity3DTree<Player> kdTree = new Entity3DTree<>();

            for (World world : enabledWorlds) {
                for (Player player : world.getPlayers()) {
                    final User user = User.getUser(player);
                    if (!User.isUserInvalid(user, Teaming.INSTANCE)
                        // Correct game modes.
                        && user.inAdventureOrSurvivalMode()
                        // Not engaged in pvp.
                        && user.getTimeMap().at(TimeKey.COMBAT).notRecentlyUpdated(noPvpTime)) {
                        final var loc = player.getLocation();
                        // Not in a bypassed region.
                        if (safeZones.stream().noneMatch(safeZone -> safeZone.isInsideRegion(loc))) kdTree.add(player);
                    }
                }

                while (!kdTree.isEmpty()) {
                    final List<Player> potentialTeam = kdTree.searchAroundAnyAndRemove(proximityRange);

                    // Team is too big
                    final int vl = potentialTeam.size() - allowedSize;
                    if (vl <= 0) continue;

                    for (final var player : potentialTeam) this.getManagement().flag(Flag.of(player).setAddedVl(vl));
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

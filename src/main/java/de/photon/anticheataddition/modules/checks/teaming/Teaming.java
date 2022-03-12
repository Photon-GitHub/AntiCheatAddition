package de.photon.anticheataddition.modules.checks.teaming;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimestampKey;
import de.photon.anticheataddition.util.datastructure.kdtree.QuadTreeSet;
import de.photon.anticheataddition.util.minecraft.world.Region;
import de.photon.anticheataddition.util.violationlevels.Flag;
import de.photon.anticheataddition.util.violationlevels.ViolationLevelManagement;
import de.photon.anticheataddition.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.Set;
import java.util.stream.Collectors;

public class Teaming extends ViolationModule implements Listener
{
    private final Set<Region> safeZones = loadStringList(".safe_zones")
            .stream()
            .map(Region::parseRegion)
            .collect(Collectors.toUnmodifiableSet());

    private final Set<World> enabledWorlds = loadStringList(".enabled_worlds")
            .stream()
            .map(key -> Preconditions.checkNotNull(Bukkit.getWorld(key), "Config loading error: Unable to identify world for the teaming check. Please check your world names listed in the config."))
            .collect(Collectors.toUnmodifiableSet());

    private final double proximityRange = loadDouble(".proximity_range", 4.5);
    private final int noPvpTime = loadInt(".no_pvp_time", 6000);
    private final int allowedSize = loadInt(".allowed_size", 1);

    public Teaming()
    {
        super("Teaming");
    }

    private boolean playerNotInSafeZone(Player player)
    {
        return this.safeZones.stream().noneMatch(safeZone -> safeZone.isInsideRegion(player.getLocation()));
    }

    @Override
    public void enable()
    {
        val period = (AntiCheatAddition.getInstance().getConfig().getInt(this.getConfigString() + ".delay") * 20L) / 1000L;
        Preconditions.checkArgument(allowedSize > 0, "The Teaming allowed_size must be greater than 0.");

        Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AntiCheatAddition.getInstance(),
                () -> {
                    val players = new QuadTreeSet<Player>();

                    for (World world : enabledWorlds) {
                        // No need to clear playersOfWorld here, that is automatically done below.
                        // Add the users of the world.
                        for (Player player : world.getPlayers()) {
                            val user = User.getUser(player);

                            // Only add users if they meet the preconditions
                            // User has to be online and not bypassed
                            if (!User.isUserInvalid(user, this) &&
                                // Correct gamemodes
                                user.inAdventureOrSurvivalMode() &&
                                // Not engaged in pvp
                                user.getTimestampMap().at(TimestampKey.TEAMING_COMBAT_TAG).notRecentlyUpdated(noPvpTime) &&
                                // Not in a bypassed region
                                playerNotInSafeZone(player))
                            {
                                players.add(player.getLocation().getX(), player.getLocation().getZ(), player);
                            }
                        }

                        while (!players.isEmpty()) {
                            // Use getAny() so the node itself is contained in the team below.
                            var firstNode = players.getAny();
                            val proximityRangeSquared = proximityRange * proximityRange;
                            val team = players.queryCircle(firstNode, proximityRange).stream()
                                              // Check for y-distance.
                                              .filter(node -> node.getElement().getLocation().distanceSquared(firstNode.getElement().getLocation()) <= proximityRangeSquared)
                                              .peek(players::remove)
                                              .map(QuadTreeSet.Node::getElement)
                                              .collect(Collectors.toUnmodifiableSet());

                            // Team is too big
                            if (team.size() > this.allowedSize) {
                                final int vl = team.size() - this.allowedSize;
                                for (Player player : team) this.getManagement().flag(Flag.of(player).setAddedVl(vl));
                            }
                        }
                    }
                }, 1L, period);
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

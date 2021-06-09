package de.photon.aacadditionpro.modules.checks.teaming;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.config.ConfigUtils;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import de.photon.aacadditionpro.util.world.Region;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class Teaming extends ViolationModule implements Listener
{
    @Getter
    private static final Teaming instance = new Teaming();

    // Region handling
    private final Set<World> enabledWorlds = new HashSet<>(3);
    private final Set<Region> safeZones = new HashSet<>(3);
    // Config
    @LoadFromConfiguration(configPath = ".proximity_range")
    private double proximityRangeSquared;
    @LoadFromConfiguration(configPath = ".no_pvp_time")
    private int noPvpTime;
    @LoadFromConfiguration(configPath = ".allowed_size")
    private int allowedSize;

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return null;
    }

    @Override
    public void enable()
    {
        final long period = (AACAdditionPro.getInstance().getConfig().getInt(this.getConfigString() + ".delay") * 20L) / 1000;

        // Square it
        proximityRangeSquared *= proximityRangeSquared;

        // Enabled worlds init
        for (final String nameOfWorld : ConfigUtils.loadImmutableStringOrStringList(this.getConfigString() + ".enabled_worlds")) {
            enabledWorlds.add(Objects.requireNonNull(Bukkit.getWorld(nameOfWorld), "Config loading error: Unable to get world " + nameOfWorld + " for the teaming check."));
        }

        // Safe zone init
        for (final String safeZone : ConfigUtils.loadImmutableStringOrStringList(this.getConfigString() + ".safe_zones")) {
            safeZones.add(Region.parseRegion(safeZone));
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AACAdditionPro.getInstance(),
                () -> {
                    // Have the same LinkedList for all worlds in order to boost performance
                    final LinkedList<User> usersOfWorld = new LinkedList<>();
                    for (final World world : enabledWorlds) {
                        // Clear the old world's data.
                        usersOfWorld.clear();

                        // Add the users of the world.
                        for (final Player player : world.getPlayers()) {
                            final User user = UserManager.getUser(player.getUniqueId());

                            // Only add users if they meet the preconditions
                            // User has to be online and not bypassed
                            if (!User.isUserInvalid(user, this.getModuleType()) &&
                                // Correct gamemodes
                                user.inAdventureOrSurvivalMode() &&
                                // Not engaged in pvp
                                !user.getTimestampMap().recentlyUpdated(TimestampKey.TEAMING_COMBAT_TAG, noPvpTime) &&
                                // Not in a bypassed region
                                !this.isPlayerRegionalBypassed(user.getPlayer()))
                            {
                                usersOfWorld.add(user);
                            }
                        }

                        while (!usersOfWorld.isEmpty()) {
                            // More than 8 players usually don't team.
                            final List<User> teamingList = new ArrayList<>(8);
                            final User currentUser = usersOfWorld.removeFirst();

                            // Add the user himself
                            teamingList.add(currentUser);

                            for (final User possibleTeamUser : usersOfWorld) {
                                if (LocationUtils.areLocationsInRange(currentUser.getPlayer().getLocation(), possibleTeamUser.getPlayer().getLocation(), proximityRangeSquared)) {
                                    usersOfWorld.remove(possibleTeamUser);
                                    teamingList.add(possibleTeamUser);
                                }
                            }

                            // Team is too big
                            if (teamingList.size() > this.allowedSize) {
                                final List<Player> playersOfTeam = new ArrayList<>(teamingList.size());

                                for (final User teamUser : teamingList) {
                                    playersOfTeam.add(teamUser.getPlayer());
                                }

                                // Flag the team
                                vlManager.flagTeam(playersOfTeam, -1, () -> {}, () -> {});
                            }
                        }
                    }
                }, 1L, period);
    }

    /**
     * @return false if the given {@link Player} is not in an enabled world or he is in a safe zone.
     */
    private boolean isPlayerRegionalBypassed(final Player player)
    {
        for (final Region safe_zone : safeZones) {
            if (safe_zone.isInsideRegion(player.getLocation())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).withDecay(300, 1).build()
    }
}

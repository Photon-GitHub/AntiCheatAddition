package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.ConfigUtils;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.storage.management.TeamViolationLevelManagement;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.world.Region;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Teaming implements Listener, ViolationModule
{
    private final TeamViolationLevelManagement vlManager = new TeamViolationLevelManagement(this.getModuleType(), 300);

    // Config
    @LoadFromConfiguration(configPath = ".proximity_range")
    private double proximity_range_squared;
    @LoadFromConfiguration(configPath = ".no_pvp_time")
    private int no_pvp_time;
    @LoadFromConfiguration(configPath = ".allowed_size")
    private int allowed_size;

    // Region handling
    private final List<World> enabled_worlds = new ArrayList<>(3);
    private final List<Region> safe_zones = new ArrayList<>(3);

    @Override
    public void subEnable()
    {
        final long period = (AACAdditionPro.getInstance().getConfig().getInt(this.getModuleType().getConfigString() + ".delay") * 20) / 1000;

        // Square it
        proximity_range_squared *= proximity_range_squared;

        // Enabled worlds init
        for (final String nameOfWorld : ConfigUtils.loadStringOrStringList(this.getModuleType().getConfigString() + ".enabled_worlds"))
        {
            enabled_worlds.add(AACAdditionPro.getInstance().getServer().getWorld(nameOfWorld));
        }

        // Safe zone init
        for (final String safe_zone : ConfigUtils.loadStringOrStringList(this.getModuleType().getConfigString() + ".safe_zones"))
        {
            safe_zones.add(new Region(safe_zone));
        }

        Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AACAdditionPro.getInstance(),
                () -> {

                    for (final World world : Bukkit.getWorlds())
                    {
                        final LinkedList<User> usersOfWorld = new LinkedList<>();

                        // Add the users of the world.
                        for (final Player player : world.getPlayers())
                        {
                            final User user = UserManager.getUser(player.getUniqueId());

                            // Only add users if they meet the preconditions
                            // User has to be online and not bypassed
                            if (!User.isUserInvalid(user) &&
                                // Correct gamemodes
                                user.getPlayer().getGameMode() != GameMode.CREATIVE &&
                                user.getPlayer().getGameMode() != GameMode.CREATIVE &&
                                // Not engaged in pvp
                                !user.getTeamingData().recentlyUpdated(no_pvp_time) &&
                                // Not in a bypassed region
                                !this.isPlayerRegionalBypassed(user.getPlayer()))
                            {
                                usersOfWorld.add(user);
                            }
                        }

                        // More than 8 players usually don't team.
                        final List<User> teamingList = new ArrayList<>(8);
                        final User currentUser = usersOfWorld.removeFirst();

                        // Add the user himself
                        teamingList.add(currentUser);

                        for (final User possibleTeamUser : usersOfWorld)
                        {
                            if (MathUtils.areLocationsInRange(currentUser.getPlayer().getLocation(), possibleTeamUser.getPlayer().getLocation(), proximity_range_squared))
                            {
                                usersOfWorld.remove(possibleTeamUser);
                                teamingList.add(possibleTeamUser);
                            }
                        }

                        // Team is too big
                        if (teamingList.size() > this.allowed_size)
                        {
                            final List<Player> playersOfTeam = new ArrayList<>(teamingList.size());

                            for (final User teamUser : teamingList)
                            {
                                playersOfTeam.add(teamUser.getPlayer());
                            }

                            // Flag the team
                            vlManager.flagTeam(playersOfTeam, -1, () -> {}, () -> {});
                        }
                    }
                }, 1L, period);
    }

    /**
     * @return false if the given {@link Player} is not in an enabled world or he is in a safe zone.
     */
    private boolean isPlayerRegionalBypassed(final Player player)
    {
        if (enabled_worlds.contains(player.getWorld()))
        {
            for (final Region safe_zone : safe_zones)
            {
                if (safe_zone.isInsideRegion(player.getLocation()))
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public ViolationLevelManagement getViolationLevelManagement()
    {
        return vlManager;
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.TEAMING;
    }
}

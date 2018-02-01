package de.photon.AACAdditionPro.checks.subchecks;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.checks.ViolationModule;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.userdata.UserManager;
import de.photon.AACAdditionPro.util.files.ConfigUtils;
import de.photon.AACAdditionPro.util.files.LoadFromConfiguration;
import de.photon.AACAdditionPro.util.storage.management.TeamViolationLevelManagement;
import de.photon.AACAdditionPro.util.storage.management.ViolationLevelManagement;
import de.photon.AACAdditionPro.util.world.EntityUtils;
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

        Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), this::repeatingTeamingTask, 1L, period);
    }

    /**
     * This is called after the period.
     */
    private void repeatingTeamingTask()
    {
        final LinkedList<Player> currentUsers = new LinkedList<>(Bukkit.getOnlinePlayers());

        while (!currentUsers.isEmpty())
        {
            final User user = UserManager.getUser(currentUsers.removeFirst().getUniqueId());

            // User is ok.
            if (this.userMeetsPreconditions(user))
            {
                final List<User> teamOfCurrentUser = new ArrayList<>(5);

                // Add the user himself
                teamOfCurrentUser.add(user);

                // The initial User is not returned, thus one does not need to remove the user above here.
                final List<Player> nearbyPlayers = EntityUtils.getNearbyPlayers(user.getPlayer(), proximity_range_squared);

                for (final Player nearbyPlayer : nearbyPlayers)
                {
                    final User nearUser = UserManager.getUser(nearbyPlayer.getUniqueId());

                    // User is ok
                    if (this.userMeetsPreconditions(nearUser) &&
                        // User has had no pvp
                        !nearUser.getTeamingData().recentlyUpdated(no_pvp_time))
                    {
                        currentUsers.remove(nearUser.getPlayer());
                        teamOfCurrentUser.add(nearUser);
                    }
                }

                // Team is too big
                if (teamOfCurrentUser.size() > this.allowed_size)
                {
                    final List<Player> playersOfTeam = new ArrayList<>(teamOfCurrentUser.size());

                    for (final User teamUser : teamOfCurrentUser)
                    {
                        playersOfTeam.add(teamUser.getPlayer());
                    }

                    // Flag the team
                    vlManager.flagTeam(playersOfTeam, -1, () -> {}, () -> {});
                }
            }
        }
    }

    /**
     * Determines whether {@link AACAdditionPro} should check this {@link User}
     */
    private boolean userMeetsPreconditions(final User user)
    {
        // User has to be online
        return user != null &&
               // User must not be bypassed
               !user.isBypassed() &&
               // User must be in the right GameMode
               user.getPlayer().getGameMode() != GameMode.CREATIVE &&
               user.getPlayer().getGameMode() != GameMode.CREATIVE &&
               // Player must be in an enabled world and must not be in a safe zone
               !this.isPlayerRegionalBypassed(user.getPlayer());
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

package de.photon.aacadditionpro.modules.checks.teaming;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.config.ConfigUtils;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import de.photon.aacadditionpro.util.world.LocationUtil;
import de.photon.aacadditionpro.util.world.Region;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class Teaming extends ViolationModule implements Listener
{
    @LoadFromConfiguration(configPath = ".proximity_range")
    private double proximityRangeSquared;
    @LoadFromConfiguration(configPath = ".no_pvp_time")
    private int noPvpTime;
    @LoadFromConfiguration(configPath = ".allowed_size")
    private int allowedSize;

    public Teaming()
    {
        super("Teaming");
    }

    @Override
    public void enable()
    {
        val period = (AACAdditionPro.getInstance().getConfig().getInt(this.getConfigString() + ".delay") * 20L) / 1000;

        // Square it
        proximityRangeSquared *= proximityRangeSquared;

        final ImmutableSet.Builder<World> worldBuilder = new ImmutableSet.Builder<>();
        final ImmutableSet.Builder<Region> safeZoneBuilder = new ImmutableSet.Builder<>();

        ConfigUtils.loadImmutableStringOrStringList(this.getConfigString() + ".enabled_worlds").stream()
                   .map(Bukkit::getWorld)
                   .forEach(world -> worldBuilder.add(Preconditions.checkNotNull(world, "Config loading error: Unable to identify world for the teaming check. Please check your world names listed in the config.")));

        ConfigUtils.loadImmutableStringOrStringList(this.getConfigString() + ".safe_zones").stream().map(Region::parseRegion).forEach(safeZoneBuilder::add);

        final Set<World> enabledWorlds = worldBuilder.build();
        final Set<Region> safeZones = safeZoneBuilder.build();

        Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AACAdditionPro.getInstance(),
                () -> {
                    final LinkedList<Player> playersOfWorld = new LinkedList<>();
                    final List<Player> teamingList = new LinkedList<>();

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
                                !user.getTimestampMap().at(TimestampKey.TEAMING_COMBAT_TAG).recentlyUpdated(noPvpTime) &&
                                // Not in a bypassed region
                                safeZones.stream().noneMatch(safeZone -> safeZone.isInsideRegion(player.getLocation())))
                            {
                                playersOfWorld.add(user.getPlayer());
                            }
                        }

                        while (!playersOfWorld.isEmpty()) {
                            teamingList.clear();
                            val currentPlayer = playersOfWorld.removeFirst();
                            teamingList.add(currentPlayer);

                            for (final Player possibleTeamPlayer : playersOfWorld) {
                                if (LocationUtil.areLocationsInRange(currentPlayer.getLocation(), possibleTeamPlayer.getLocation(), proximityRangeSquared)) {
                                    playersOfWorld.remove(possibleTeamPlayer);
                                    teamingList.add(possibleTeamPlayer);
                                }
                            }

                            // Team is too big
                            if (teamingList.size() > this.allowedSize) this.getManagement().flag(Flag.of(ImmutableSet.copyOf(teamingList)));
                        }
                        teamingList.clear();
                    }
                }, 1L, period);
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).withDecay(300, 1).build();
    }
}

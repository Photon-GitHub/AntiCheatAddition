package de.photon.aacadditionpro.modules.checks.teaming;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.config.ConfigUtils;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.minecraft.world.Region;
import de.photon.aacadditionpro.util.minecraft.world.WorldUtil;
import de.photon.aacadditionpro.util.violationlevels.Flag;
import de.photon.aacadditionpro.util.violationlevels.ViolationLevelManagement;
import de.photon.aacadditionpro.util.violationlevels.ViolationManagement;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Teaming extends ViolationModule implements Listener
{
    private final Set<Region> safeZones = ConfigUtils.loadImmutableStringOrStringList(this.getConfigString() + ".safe_zones").stream()
                                                     .map(Region::parseRegion)
                                                     .collect(Collectors.toUnmodifiableSet());

    private final Set<World> enabledWorlds = ConfigUtils.loadImmutableStringOrStringList(this.getConfigString() + ".enabled_worlds").stream()
                                                        .map(key -> Preconditions.checkNotNull(Bukkit.getWorld(key), "Config loading error: Unable to identify world for the teaming check. Please check your world names listed in the config."))
                                                        .collect(Collectors.toUnmodifiableSet());

    @LoadFromConfiguration(configPath = ".proximity_range")
    private double proximityRange;
    @LoadFromConfiguration(configPath = ".no_pvp_time")
    private int noPvpTime;
    @LoadFromConfiguration(configPath = ".allowed_size")
    private int allowedSize;

    public Teaming()
    {
        super("Teaming");
    }

    private boolean playerNotInSafeZone(Player player)
    {
        val location = player.getLocation();
        for (Region safeZone : this.safeZones) {
            if (safeZone.isInsideRegion(location)) return false;
        }
        return true;
    }

    @Override
    public void enable()
    {
        val period = (AACAdditionPro.getInstance().getConfig().getInt(this.getConfigString() + ".delay") * 20L) / 1000L;

        Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AACAdditionPro.getInstance(),
                () -> {
                    // TODO: USE KD-TREE HERE.
                    val playersOfWorld = new TeamingSet();

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
                                !playerNotInSafeZone(player))
                            {
                                playersOfWorld.addPlayer(player);
                            }
                        }

                        val teamingList = new ArrayList<Player>();
                        while (!playersOfWorld.isEmpty()) {
                            teamingList.clear();

                            var entry = playersOfWorld.removeFirst();
                            teamingList.add(entry.getValue());

                            // Now, the first iterator element is the entry above our "first" entry above.
                            // Use nextEntries to automatically ignore all entries after the proximityRange.
                            var iterator = playersOfWorld.nextEntries(entry.getKey(), proximityRange).iterator();
                            while (iterator.hasNext()) {
                                var higherEntry = iterator.next();

                                if (WorldUtil.INSTANCE.areLocationsInRange(entry.getValue().getLocation(), higherEntry.getValue().getLocation(), proximityRange)) {
                                    teamingList.add(higherEntry.getValue());
                                    iterator.remove();
                                }
                            }

                            // Team is too big
                            if (teamingList.size() > this.allowedSize) this.getManagement().flag(Flag.of(Set.copyOf(teamingList)));
                        }
                    }
                }, 1L, period);
    }

    private void insertPlayer(TreeMap<Double, Player> playersOfWorld, Player player)
    {

    }

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).loadThresholdsToManagement().withDecay(300, 1).build();
    }

    private static class TeamingSet implements Iterable<Map.Entry<Double, Player>>
    {
        private final Random random = new Random();
        private final TreeMap<Double, Player> playerMap = new TreeMap<>();

        public boolean isEmpty()
        {
            return playerMap.isEmpty();
        }

        public void addPlayer(Player player)
        {
            val x = player.getLocation().getX();
            var old = playerMap.put(x, player);
            // If equal, place somewhere near.
            // If the randomness is not sufficient, stop after 10 iterations.
            for (int i = 0; i < 10 && old != null; i++) {
                old = playerMap.put(x + (this.random.nextDouble() / 10), old);
            }
        }

        public Map.Entry<Double, Player> removeFirst()
        {
            var entry = playerMap.firstEntry();
            this.playerMap.remove(entry.getKey());
            return entry;
        }

        public Set<Map.Entry<Double, Player>> nextEntries(double fromKey, double range)
        {
            return this.playerMap.headMap(fromKey + range).entrySet();
        }

        @NotNull
        @Override
        public Iterator<Map.Entry<Double, Player>> iterator()
        {
            return this.playerMap.entrySet().iterator();
        }
    }
}

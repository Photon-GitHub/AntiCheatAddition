package de.photon.aacadditionpro.modules.checks.teaming;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.ViolationModule;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.data.TimestampKey;
import de.photon.aacadditionpro.util.config.ConfigUtils;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.mathematics.MathUtil;
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

import java.util.ArrayList;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class Teaming extends ViolationModule implements Listener
{
    private final Random random = new Random();

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
        for (Region safeZone : this.safeZones) {
            if (safeZone.isInsideRegion(player.getLocation())) return false;
        }
        return true;
    }

    @Override
    public void enable()
    {
        val period = (AACAdditionPro.getInstance().getConfig().getInt(this.getConfigString() + ".delay") * 20L) / 1000;

        Bukkit.getScheduler().scheduleSyncRepeatingTask(
                AACAdditionPro.getInstance(),
                () -> {
                    // TODO: USE KD-TREE HERE.
                    val playersOfWorld = new TreeMap<Double, Player>();
                    val teamingList = new ArrayList<Player>();

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
                                val x = player.getLocation().getX();
                                var old = playersOfWorld.put(x, player);
                                // If equal, place somewhere near.
                                // If the randomness is not sufficient, stop after 10 iterations.
                                for (int i = 0; i < 10 && old != null; i++) {
                                    old = playersOfWorld.put(x + (this.random.nextDouble() / 10), old);
                                }
                            }
                        }

                        while (!playersOfWorld.isEmpty()) {
                            teamingList.clear();

                            var entry = playersOfWorld.firstEntry();
                            playersOfWorld.remove(entry.getKey());
                            teamingList.add(entry.getValue());

                            for (var iterator = playersOfWorld.entrySet().iterator(); iterator.hasNext(); ) {
                                var higherEntry = iterator.next();

                                // No need to check the others after we are beyond our proximity range.
                                if (!MathUtil.roughlyEquals(entry.getKey(), higherEntry.getKey(), proximityRange)) break;

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

    @Override
    protected ViolationManagement createViolationManagement()
    {
        return ViolationLevelManagement.builder(this).loadThresholdsToManagement().withDecay(300, 1).build();
    }
}

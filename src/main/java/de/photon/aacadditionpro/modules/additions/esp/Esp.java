package de.photon.aacadditionpro.modules.additions.esp;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.config.Configs;
import de.photon.aacadditionpro.util.config.LoadFromConfiguration;
import de.photon.aacadditionpro.util.datastructure.ImmutablePair;
import de.photon.aacadditionpro.util.visibility.PlayerVisibility;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Function;

public class Esp extends Module
{
    // This defines the max tracking range supported by Esp.
    // Also the max distance of a BlockIterator.
    public static final Function<Player, Vector[]> CAMERA_VECTOR_SUPPLIER = AACAdditionPro.getInstance().getConfig().getBoolean("Esp.calculate_third_person_modes", true) ?
                                                                            new CanSeeThirdPerson() :
                                                                            new CanSeeNoThirdPerson();

    private static final int MAX_TRACKING_RANGE = 139 * 139;
    private static final String DEFAULT_WORLD_NAME = "default";

    private int defaultTrackingRange;
    private ImmutableMap<World, Integer> playerTrackingRanges;


    @LoadFromConfiguration(configPath = ".inverval")
    private long interval;

    @LoadFromConfiguration(configPath = ".calculate_third_person_modes")
    private boolean thirdPerson;

    public Esp()
    {
        super("Esp");
    }

    @Override
    protected void enable()
    {
        // ---------------------------------------------------- Auto-configuration ----------------------------------------------------- //
        val worlds = Preconditions.checkNotNull(Configs.SPIGOT.getConfigurationRepresentation().getYamlConfiguration().getConfigurationSection("world-settings"), "World settings are not present. Aborting ESP enable.");
        final ImmutableMap.Builder<World, Integer> rangeBuilder = ImmutableMap.builder();

        val worldKeys = worlds.getKeys(false);
        defaultTrackingRange = worldKeys.contains(DEFAULT_WORLD_NAME) ? worlds.getInt(DEFAULT_WORLD_NAME + ".entity-tracking-range.players") : MAX_TRACKING_RANGE;

        worldKeys.stream()
                 .filter(key -> !DEFAULT_WORLD_NAME.equals(key))
                 .map(key -> {
                     val trackingRange = worlds.getInt(key + ".entity-tracking-range.players");
                     // Squared distance.
                     return ImmutablePair.of(Bukkit.getWorld(key), trackingRange * trackingRange);
                 })
                 .filter(pair -> pair.getSecond() < MAX_TRACKING_RANGE)
                 .forEach(pair -> rangeBuilder.put(pair.getFirst(), pair.getSecond()));

        this.playerTrackingRanges = rangeBuilder.build();

        // ----------------------------------------------------------- Task ------------------------------------------------------------ //

        Bukkit.getScheduler().runTaskTimerAsynchronously(AACAdditionPro.getInstance(), () -> {
            final Deque<Player> players = new ArrayDeque<>();
            Player observer;

            for (World world : Bukkit.getWorlds()) {
                for (Player player : world.getPlayers()) {
                    //noinspection ConstantConditions
                    if (player.getWorld() != null && player.getGameMode() != GameMode.SPECTATOR && !User.isUserInvalid(User.getUser(player), this)) players.add(player);
                }

                while (!players.isEmpty()) {
                    // Remove the finished player to reduce the amount of added entries.
                    // This makes sure the player won't have a connection with himself.
                    // Remove the last object for better array performance.
                    observer = players.removeLast();
                    for (Player watched : players) handlePair(observer, watched);
                }
            }
        }, 100, interval);
    }

    private void handlePair(Player first, Player second)
    {
        final int playerTrackingRange = this.playerTrackingRanges.getOrDefault(first.getWorld(), this.defaultTrackingRange);

        // The users are always in the same world (see above)
        val pairDistanceSquared = first.getLocation().distanceSquared(second.getLocation());

        // Less than 1 block distance
        // Everything (smaller than 1)^2 will result in something smaller than 1
        if (pairDistanceSquared < 1) {
            PlayerVisibility.revealPlayer(first, second);
            PlayerVisibility.revealPlayer(second, first);
        } else if (pairDistanceSquared >= playerTrackingRange) {
            PlayerVisibility.fullyHidePlayer(first, second);
            PlayerVisibility.fullyHidePlayer(second, first);
        } else {
            handleDirection(first, second);
            handleDirection(second, first);
        }
    }

    private void handleDirection(Player observer, Player watched)
    {
        // Is the user visible
        if (CanSee.canSee(observer, watched)) PlayerVisibility.revealPlayer(observer, watched);
        else {
            if (watched.isSneaking()) PlayerVisibility.fullyHidePlayer(observer, watched);
            else PlayerVisibility.hideEquipment(observer, watched);
        }
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }
}


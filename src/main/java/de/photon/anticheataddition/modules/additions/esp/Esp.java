package de.photon.anticheataddition.modules.additions.esp;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.config.Configs;
import de.photon.anticheataddition.util.datastructure.kdtree.QuadTreeQueue;
import de.photon.anticheataddition.util.messaging.DebugSender;
import de.photon.anticheataddition.util.visibility.PlayerVisibility;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Esp extends Module
{
    public static final long ESP_INTERVAL_TICKS = AntiCheatAddition.getInstance().getConfig().getLong("Esp.interval_ticks", 2L);

    public static final String ENTITY_TRACKING_RANGE_PLAYERS = ".entity-tracking-range.players";
    private static final String DEFAULT_WORLD_NAME = "default";

    private static final int MAX_TRACKING_RANGE = 139;

    public Esp()
    {
        super("Esp");
    }

    @Override
    protected void enable()
    {
        // ---------------------------------------------------- Auto-configuration ----------------------------------------------------- //
        val worlds = Preconditions.checkNotNull(Configs.SPIGOT.getConfigurationRepresentation().getYamlConfiguration().getConfigurationSection("world-settings"), "World settings are not present. Aborting ESP enable.");
        val worldKeys = worlds.getKeys(false);

        final int defaultTrackingRange = loadDefaultTrackingRange(worlds);
        final Map<World, Integer> playerTrackingRanges = loadWorldTrackingRanges(worlds, worldKeys);

        // ----------------------------------------------------------- Task ------------------------------------------------------------ //

        Bukkit.getScheduler().runTaskTimerAsynchronously(AntiCheatAddition.getInstance(), () -> {
            for (World world : Bukkit.getWorlds()) {
                final int playerTrackingRange = playerTrackingRanges.getOrDefault(world, defaultTrackingRange);

                val worldPlayers = world.getPlayers().stream()
                                        .filter(player -> player.getWorld() != null)
                                        .map(User::getUser)
                                        .filter(user -> !User.isUserInvalid(user, this))
                                        .filter(User::inAdventureOrSurvivalMode)
                                        .map(User::getPlayer)
                                        .collect(Collectors.toUnmodifiableSet());

                val playerQuadTree = new QuadTreeQueue<Player>();
                for (Player player : worldPlayers) playerQuadTree.add(player.getLocation().getX(), player.getLocation().getZ(), player);

                for (var observerNode : playerQuadTree) {
                    final Set<Entity> equipHiddenPlayers = new HashSet<>();
                    final Set<Entity> fullHiddenPlayers = new HashSet<>(worldPlayers);
                    final Player observer = observerNode.getElement();

                    for (var playerNode : playerQuadTree.queryCircle(observerNode, playerTrackingRange)) {
                        final Player watched = playerNode.getElement();

                        // Less than 1 block distance (removes the player themselves and any very close player)
                        if (observerNode.distanceSquared(playerNode) < 1 || CanSee.canSee(observer, watched)) {
                            // No hiding case
                            fullHiddenPlayers.remove(watched);
                        } else if (!watched.isSneaking()) {
                            // Equip hiding
                            fullHiddenPlayers.remove(watched);
                            equipHiddenPlayers.add(watched);
                        }
                        // Full hiding (due to the default adding to fullHiddenPlayers.)
                    }

                    PlayerVisibility.INSTANCE.setFullyHidden(observerNode.getElement(), fullHiddenPlayers);
                    PlayerVisibility.INSTANCE.setEquipmentHidden(observerNode.getElement(), equipHiddenPlayers);
                }
                playerQuadTree.clear();
            }
        }, 100, ESP_INTERVAL_TICKS);
    }

    private int loadDefaultTrackingRange(ConfigurationSection worlds)
    {
        if (worlds.contains(DEFAULT_WORLD_NAME + ENTITY_TRACKING_RANGE_PLAYERS)) {
            DebugSender.getInstance().sendDebug("ESP | Default entity tracking range found.");
            return worlds.getInt(DEFAULT_WORLD_NAME + ENTITY_TRACKING_RANGE_PLAYERS);
        } else {
            DebugSender.getInstance().sendDebug("ESP | Default entity tracking range not found, using max tracking range.");
            return MAX_TRACKING_RANGE;
        }
    }

    @NotNull
    private Map<World, Integer> loadWorldTrackingRanges(ConfigurationSection worlds, Set<String> worldKeys)
    {
        val playerTrackingRanges = new HashMap<World, Integer>();
        for (String key : worldKeys) {
            // Skip the default world range.
            if (DEFAULT_WORLD_NAME.equals(key)) continue;

            // Does the world exist?
            val world = Bukkit.getWorld(key);
            if (world == null || !worlds.contains(key + ENTITY_TRACKING_RANGE_PLAYERS)) {
                DebugSender.getInstance().sendDebug("ESP | World " + key + " player tracking range could not be loaded, using default tracking range.");
                continue;
            }

            final int trackingRange = worlds.getInt(key + ENTITY_TRACKING_RANGE_PLAYERS);

            // Is the tracking range smaller than the max tracking range?
            if (trackingRange < MAX_TRACKING_RANGE) playerTrackingRanges.put(Bukkit.getWorld(key), trackingRange);
        }
        return Map.copyOf(playerTrackingRanges);
    }
}


package de.photon.anticheataddition.modules.additions.esp;

import com.github.davidmoten.rtreemulti.Entry;
import com.github.davidmoten.rtreemulti.RTree;
import com.github.davidmoten.rtreemulti.geometry.Point;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.config.Configs;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.visibility.PlayerVisibility;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Module for Extra Sensory Perception (ESP) detection and prevention.
 * Manages player visibility based on in-game mechanics to prevent wallhacks and similar cheats.
 */
public final class Esp extends Module {

    public static final Esp INSTANCE = new Esp();

    /** How often (in ticks) to update player visibility */
    public static final long ESP_INTERVAL_TICKS = Esp.INSTANCE.loadLong(".interval_ticks", 2L);

    /** Whether to only perform full player hiding (true) or also equipment hiding (false) */
    private static final boolean ONLY_FULL_HIDE = Esp.INSTANCE.loadBoolean(".only_full_hide", false);

    /** Configuration path for player tracking range */
    private static final String ENTITY_TRACKING_RANGE_PLAYERS = ".entity-tracking-range.players";

    /** Default world name in spigot configuration */
    private static final String DEFAULT_WORLD_NAME = "default";

    /** Maximum tracking range to use if configuration values exceed this */
    private static final int MAX_TRACKING_RANGE = 139;

    private Esp() {
        super("Esp");
    }

    /**
     * Loads the default entity tracking range from Spigot configuration.
     * Falls back to maximum tracking range if not found.
     *
     * @param worlds The world-settings configuration section
     * @return The default tracking range value
     */
    private static int loadDefaultTrackingRange(ConfigurationSection worlds) {
        if (worlds.contains(DEFAULT_WORLD_NAME + ENTITY_TRACKING_RANGE_PLAYERS)) {
            Log.info(() -> "ESP | Default entity tracking range found.");
            return worlds.getInt(DEFAULT_WORLD_NAME + ENTITY_TRACKING_RANGE_PLAYERS);
        } else {
            Log.info(() -> "ESP | Default entity tracking range not found, using max tracking range.");
            return MAX_TRACKING_RANGE;
        }
    }

    /**
     * Loads world-specific tracking ranges from the Spigot configuration.
     *
     * @param worlds The world-settings configuration section
     * @param worldKeys The set of world names from configuration
     * @return Map of worlds to their tracking ranges
     */
    @NotNull
    private static Map<World, Integer> loadWorldTrackingRanges(ConfigurationSection worlds, Set<String> worldKeys) {
        final var playerTrackingRanges = new HashMap<World, Integer>();

        for (String key : worldKeys) {
            // Skip the default world range
            if (DEFAULT_WORLD_NAME.equals(key)) continue;

            // Get the world instance
            final var world = Bukkit.getWorld(key);
            if (world == null || !worlds.contains(key + ENTITY_TRACKING_RANGE_PLAYERS)) {
                Log.warning(() -> "ESP | World " + key + " player tracking range could not be loaded, using default tracking range.");
                continue;
            }

            final int trackingRange = worlds.getInt(key + ENTITY_TRACKING_RANGE_PLAYERS);

            // Only add if the tracking range is within limits
            if (trackingRange < MAX_TRACKING_RANGE) {
                playerTrackingRanges.put(world, trackingRange);
            }
        }

        return Map.copyOf(playerTrackingRanges);
    }

    @Override
    protected void enable() {
        // Load configuration from spigot.yml
        final var worlds = Configs.SPIGOT.getConfigurationRepresentation().getYamlConfiguration().getConfigurationSection("world-settings");
        if (worlds == null) {
            Log.severe(() -> "Cannot enable ESP as the world-settings in spigot.yml are not present.");
            return;
        }

        final var worldKeys = worlds.getKeys(false);
        final int defaultTrackingRange = loadDefaultTrackingRange(worlds);
        final Map<World, Integer> playerTrackingRanges = loadWorldTrackingRanges(worlds, worldKeys);

        Log.info(() -> "ESP | OnlyFullHide: " + ONLY_FULL_HIDE);

        // Schedule the visibility update task
        Bukkit.getScheduler().runTaskTimer(AntiCheatAddition.getInstance(), () -> {
            for (World world : Bukkit.getWorlds()) {
                processWorld(world, playerTrackingRanges.getOrDefault(world, defaultTrackingRange));
            }
        }, 100, ESP_INTERVAL_TICKS);
    }

    /**
     * Processes player visibility for a specific world.
     *
     * @param world The world to process
     * @param playerTrackingRange The tracking range to use for this world
     */
    private void processWorld(World world, int playerTrackingRange) {
        // Get all valid players in this world
        final var worldPlayers = world.getPlayers().stream()
                .map(User::getUser)
                .filter(user -> !User.isUserInvalid(user, this))
                .map(User::getPlayer)
                .collect(Collectors.toUnmodifiableSet());

        // Skip if there are no players
        if (worldPlayers.isEmpty()) {
            return;
        }

        // Create RTree entries for spatial indexing
        final List<Entry<Player, Point>> entries = worldPlayers.stream()
                .map(User::rTreeEntryFromPlayer)
                .collect(Collectors.toCollection(ArrayList::new));

        // Create the RTree for the world
        RTree<Player, Point> rTree = RTree.dimensions(3).create(entries);

        // Process visibility for all players
        processWorldRTree(playerTrackingRange, worldPlayers, rTree);
    }

    /**
     * Processes player visibility using the spatial index (RTree).
     *
     * @param playerTrackingRange The maximum tracking range
     * @param worldPlayers All players in this world
     * @param rTree The spatial index of players
     */
    private static void processWorldRTree(int playerTrackingRange, Set<Player> worldPlayers, RTree<Player, Point> rTree) {
        for (final var observerNode : rTree.entries()) {
            final var observer = observerNode.value();
            final var observerPoint = observerNode.geometry();
            final var observerLoc = observer.getLocation();

            // Special case for creative and spectator mode observers
            if (!User.inAdventureOrSurvivalMode(observer)) {
                Log.finest(() -> "ESP | Observer: " + observer.getName() + " | In creative or spectator mode, no hidden players.");
                PlayerVisibility.INSTANCE.setHidden(observer, Set.of(), Set.of());
                continue;
            }

<<<<<<< HEAD
            // Collections to track which players should be hidden and how
=======
>>>>>>> improvments
            final Set<Player> equipHiddenPlayers = HashSet.newHashSet(worldPlayers.size());
            final Set<Player> fullHiddenPlayers = new HashSet<>(worldPlayers);

            // Check all nearby players
            for (final var watchedNode : rTree.nearest(observerPoint, playerTrackingRange, 10000)) {
                final Player watched = watchedNode.value();

                // Determine visibility status
                if (!shouldBeHidden(observer, watched, observerLoc)) {
                    // No hiding needed
                    fullHiddenPlayers.remove(watched);
                } else if (!ONLY_FULL_HIDE && !watched.isSneaking()) {
                    // Equip hiding (only hide equipment)
                    fullHiddenPlayers.remove(watched);
                    equipHiddenPlayers.add(watched);
                }
                // Otherwise, keep in fullHiddenPlayers for complete hiding
            }

            // Log visibility decisions when in finest log level
            if (Log.INSTANCE.logger.isLoggable(Level.FINEST)) {
                logVisibilityDecisions(observer, fullHiddenPlayers, equipHiddenPlayers);
            }

            // Apply the visibility settings
            PlayerVisibility.INSTANCE.setHidden(observer, fullHiddenPlayers, equipHiddenPlayers);
        }
    }

    /**
     * Determines if a watched player should be hidden from an observer.
     *
     * @param observer The player who is looking
     * @param watched The player being looked at
     * @param observerLoc The observer's location (for optimization)
     * @return true if the watched player should be hidden, false otherwise
     */
    private static boolean shouldBeHidden(Player observer, Player watched, Location observerLoc) {
        return User.inAdventureOrSurvivalMode(watched) // Only hide players in survival/adventure
                && observerLoc.distanceSquared(watched.getLocation()) >= 1 // More than 1 block away
                && !watched.isDead() // Not dead
                && !CanSee.canSee(observer, watched); // Not visible according to game mechanics
    }

    /**
     * Logs visibility decisions for debugging purposes.
     */
    private static void logVisibilityDecisions(Player observer, Set<Player> fullHiddenPlayers, Set<Player> equipHiddenPlayers) {
        Log.finest(() -> "ESP | Observer: " + observer.getName() +
                " | FULL: " + fullHiddenPlayers.stream().map(Player::getName).collect(Collectors.joining(", ")) +
                " | EQUIP: " + equipHiddenPlayers.stream().map(Player::getName).collect(Collectors.joining(", ")));
    }
}
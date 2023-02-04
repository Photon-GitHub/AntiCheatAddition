package de.photon.anticheataddition.modules.additions.esp;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.config.Configs;
import de.photon.anticheataddition.util.datastructure.kdtree.QuadTreeQueue;
import de.photon.anticheataddition.util.messaging.Log;
import de.photon.anticheataddition.util.visibility.PlayerVisibility;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class Esp extends Module
{
    public static final Esp INSTANCE = new Esp();

    public static final long ESP_INTERVAL_TICKS = Esp.INSTANCE.loadLong(".interval_ticks", 2L);
    private static final boolean ONLY_FULL_HIDE = Esp.INSTANCE.loadBoolean(".only_full_hide", false);

    private static final String ENTITY_TRACKING_RANGE_PLAYERS = ".entity-tracking-range.players";
    private static final String DEFAULT_WORLD_NAME = "default";

    private static final int MAX_TRACKING_RANGE = 139;

    private Esp()
    {
        super("Esp");
    }

    private static int loadDefaultTrackingRange(ConfigurationSection worlds)
    {
        if (worlds.contains(DEFAULT_WORLD_NAME + ENTITY_TRACKING_RANGE_PLAYERS)) {
            Log.info(() -> "ESP | Default entity tracking range found.");
            return worlds.getInt(DEFAULT_WORLD_NAME + ENTITY_TRACKING_RANGE_PLAYERS);
        } else {
            Log.info(() -> "ESP | Default entity tracking range not found, using max tracking range.");
            return MAX_TRACKING_RANGE;
        }
    }

    @NotNull
    private static Map<World, Integer> loadWorldTrackingRanges(ConfigurationSection worlds, Set<String> worldKeys)
    {
        val playerTrackingRanges = new HashMap<World, Integer>();
        for (String key : worldKeys) {
            // Skip the default world range.
            if (DEFAULT_WORLD_NAME.equals(key)) continue;

            // Does the world exist?
            val world = Bukkit.getWorld(key);
            if (world == null || !worlds.contains(key + ENTITY_TRACKING_RANGE_PLAYERS)) {
                Log.severe(() -> "ESP | World " + key + " player tracking range could not be loaded, using default tracking range.");
                continue;
            }

            final int trackingRange = worlds.getInt(key + ENTITY_TRACKING_RANGE_PLAYERS);

            // Is the tracking range smaller than the max tracking range?
            if (trackingRange < MAX_TRACKING_RANGE) playerTrackingRanges.put(Bukkit.getWorld(key), trackingRange);
        }
        return Map.copyOf(playerTrackingRanges);
    }

    @Override
    protected void enable()
    {
        // ---------------------------------------------------- Auto-configuration ----------------------------------------------------- //
        val worlds = Configs.SPIGOT.getConfigurationRepresentation().getYamlConfiguration().getConfigurationSection("world-settings");
        if (worlds == null) {
            Log.severe(() -> "Cannot enable ESP as the world-settings in spigot.yml are not present.");
            return;
        }

        val worldKeys = worlds.getKeys(false);

        final int defaultTrackingRange = loadDefaultTrackingRange(worlds);
        final Map<World, Integer> playerTrackingRanges = loadWorldTrackingRanges(worlds, worldKeys);

        Log.fine(() -> "ESP | OnlyFullHide: " + ONLY_FULL_HIDE);

        // ----------------------------------------------------------- Task ------------------------------------------------------------ //

        Bukkit.getScheduler().runTaskTimerAsynchronously(AntiCheatAddition.getInstance(), () -> {
            for (World world : Bukkit.getWorlds()) {
                final int playerTrackingRange = playerTrackingRanges.getOrDefault(world, defaultTrackingRange);

                val worldPlayers = world.getPlayers().stream()
                                        .map(User::getUser)
                                        .filter(user -> !User.isUserInvalid(user, this))
                                        .filter(User::inAdventureOrSurvivalMode)
                                        .map(User::getPlayer)
                                        .collect(Collectors.toUnmodifiableSet());

                val playerQuadTree = new QuadTreeQueue<Player>();
                for (Player player : worldPlayers) playerQuadTree.add(player.getLocation().getX(), player.getLocation().getZ(), player);

                processWorldQuadTree(playerTrackingRange, worldPlayers, playerQuadTree);
            }
        }, 100, ESP_INTERVAL_TICKS);
    }

    private static void processWorldQuadTree(int playerTrackingRange, Set<Player> worldPlayers, QuadTreeQueue<Player> playerQuadTree)
    {
        for (val observerNode : playerQuadTree) {
            final Set<Player> equipHiddenPlayers = new HashSet<>(worldPlayers.size());
            final Set<Player> fullHiddenPlayers = new HashSet<>(worldPlayers);
            final Player observer = observerNode.element();

            for (val playerNode : playerQuadTree.queryCircle(observerNode, playerTrackingRange)) {
                final Player watched = playerNode.element();

                // Different worlds (might be possible if the player changed world in just the right moment)
                if (!observer.getWorld().getUID().equals(watched.getWorld().getUID())
                    // Less than 1 block distance (removes the player themselves and any very close player)
                    || observerNode.distanceSquared(playerNode) < 1
                    || observer.getGameMode() == GameMode.SPECTATOR
                    || observer.getGameMode() == GameMode.CREATIVE
                    || watched.isDead()
                    || CanSee.INSTANCE.canSee(observer, watched))
                {
                    // No hiding case
                    fullHiddenPlayers.remove(watched);
                } else if (!ONLY_FULL_HIDE && !watched.isSneaking()) {
                    // Equip hiding
                    fullHiddenPlayers.remove(watched);
                    equipHiddenPlayers.add(watched);
                }
                // Full hiding (due to the default adding to fullHiddenPlayers.)
            }

            Log.finest(() -> "ESP | Observer: " + observerNode.element().getName() +
                             " | FULL: " + fullHiddenPlayers.stream().map(Player::getName).collect(Collectors.joining(", ")) +
                             " | EQUIP: " + equipHiddenPlayers.stream().map(Player::getName).collect(Collectors.joining(", ")));

            PlayerVisibility.INSTANCE.setHidden(observerNode.element(), fullHiddenPlayers, equipHiddenPlayers);
        }
    }
}


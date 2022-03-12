package de.photon.anticheataddition.modules.additions.esp;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.config.Configs;
import de.photon.anticheataddition.util.datastructure.Pair;
import de.photon.anticheataddition.util.datastructure.kdtree.QuadTreeQueue;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.visibility.PlayerVisibility;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class Esp extends Module
{
    public static final long ESP_INTERVAL_TICKS = AntiCheatAddition.getInstance().getConfig().getLong("Esp.interval_ticks", 2L);

    private static final int MAX_TRACKING_RANGE = MathUtil.pow(139, 2);
    private static final String DEFAULT_WORLD_NAME = "default";

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

        final int defaultTrackingRange = worldKeys.contains(DEFAULT_WORLD_NAME) ? worlds.getInt(DEFAULT_WORLD_NAME + ".entity-tracking-range.players") : MAX_TRACKING_RANGE;

        val playerTrackingRanges = worldKeys.stream()
                                            .filter(key -> !DEFAULT_WORLD_NAME.equals(key))
                                            // Squared distance.
                                            .map(key -> Pair.of(Bukkit.getWorld(key), MathUtil.pow(worlds.getInt(key + ".entity-tracking-range.players"), 2)))
                                            // After MAX_TRACKING_RANGE, we do not need to check the full tracking range anymore.
                                            .filter(pair -> pair.getSecond() < MAX_TRACKING_RANGE)
                                            .collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond));

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
                        val watched = playerNode.getElement();

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
}


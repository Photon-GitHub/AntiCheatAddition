package de.photon.aacadditionpro.modules.additions.esp;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.config.Configs;
import de.photon.aacadditionpro.util.datastructure.Pair;
import de.photon.aacadditionpro.util.datastructure.kdtree.QuadTreeQueue;
import de.photon.aacadditionpro.util.mathematics.MathUtil;
import de.photon.aacadditionpro.util.visibility.PlayerVisibility;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

public class Esp extends Module
{
    public static final long ESP_INTERVAL = AACAdditionPro.getInstance().getConfig().getLong("Esp.interval", 50L);

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

        final Map<World, Integer> playerTrackingRanges = worldKeys.stream()
                                                                  .filter(key -> !DEFAULT_WORLD_NAME.equals(key))
                                                                  // Squared distance.
                                                                  .map(key -> Pair.of(Bukkit.getWorld(key), MathUtil.pow(worlds.getInt(key + ".entity-tracking-range.players"), 2)))
                                                                  // After MAX_TRACKING_RANGE, we do not need to check the full tracking range anymore.
                                                                  .filter(pair -> pair.getSecond() < MAX_TRACKING_RANGE)
                                                                  .collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond));

        // ----------------------------------------------------------- Task ------------------------------------------------------------ //

        Bukkit.getScheduler().runTaskTimerAsynchronously(AACAdditionPro.getInstance(), () -> {
            val players = new QuadTreeQueue<Player>();

            val fullHiddenPlayers = new HashSet<Entity>();
            val equipHiddenPlayers = new HashSet<Entity>();

            for (World world : Bukkit.getWorlds()) {
                final int playerTrackingRange = playerTrackingRanges.getOrDefault(world, defaultTrackingRange);

                val worldPlayers = world.getPlayers();
                for (Player player : worldPlayers) {
                    //noinspection ConstantConditions
                    if (player.getWorld() != null && player.getGameMode() != GameMode.SPECTATOR && !User.isUserInvalid(User.getUser(player), this)) players.add(player.getLocation().getX(), player.getLocation().getY(), player);
                }

                while (!players.isEmpty()) {
                    // Remove the finished player to reduce the amount of added entries.
                    // This makes sure the player won't have a connection with himself.
                    // Remove the last object for better array performance.
                    var observer = players.removeAny();

                    equipHiddenPlayers.clear();
                    fullHiddenPlayers.clear();
                    fullHiddenPlayers.addAll(worldPlayers);

                    for (var playerNode : players.queryCircle(observer, playerTrackingRange)) {
                        var player = playerNode.getElement();

                        switch (handlePair(observer.getElement(), player)) {
                            case FULL: break;
                            case EQUIP:
                                fullHiddenPlayers.remove(player);
                                equipHiddenPlayers.add(player);
                                break;
                            case NONE:
                                fullHiddenPlayers.remove(player);
                                break;
                        }
                    }

                    PlayerVisibility.INSTANCE.setFullyHidden(observer.getElement(), fullHiddenPlayers);
                    PlayerVisibility.INSTANCE.setEquipmentHidden(observer.getElement(), equipHiddenPlayers);
                }
            }
        }, 100, ESP_INTERVAL);
    }

    private Hidden handlePair(Player observer, Player hidden)
    {
        // The users are always in the same world (see above)
        val pairDistanceSquared = observer.getLocation().distanceSquared(hidden.getLocation());

        // Less than 1 block distance
        if (pairDistanceSquared < 1 || CanSee.canSee(observer, hidden)) return Hidden.NONE;
        return hidden.isSneaking() ? Hidden.EQUIP : Hidden.FULL;
    }

    private enum Hidden
    {
        FULL,
        EQUIP,
        NONE
    }
}


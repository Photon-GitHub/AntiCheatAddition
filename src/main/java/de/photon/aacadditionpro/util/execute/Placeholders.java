package de.photon.aacadditionpro.util.execute;

import com.google.common.base.Preconditions;
import de.photon.aacadditionproold.util.files.configs.StringUtil;
import de.photon.aacadditionproold.util.server.ServerUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Placeholders
{
    /**
     * Applies a placeholder to a {@link String}.
     *
     * @param original the original {@link String} containing all placeholders
     *
     * @return original with the placeholder replaced.
     */
    public static String replacePlaceholders(String original, Collection<Player> players)
    {
        Preconditions.checkArgument(!players.isEmpty(), "Tried to replace placeholders without players.");

        final Player player = players.iterator().next();
        final World world = player.getWorld();

        final StringBuilder placeholderBuilder = new StringBuilder();
        final StringBuilder result = new StringBuilder();
        boolean placeholderStarted = false;

        for (char c : original.toCharArray()) {
            if (c == '{') {
                // Clear any old placeholder that is still present.
                placeholderBuilder.delete(0, placeholderBuilder.length());
                // Start the recording of the placeholder
                placeholderStarted = true;
                // Make sure the '{' char is not recorded.
                continue;
            }

            if (c == '}') {
                // End the recording of the placeholder
                placeholderStarted = false;

                // See if the recorded chars match a placeholder
                switch (placeholderBuilder.toString()) {
                    // Usually one placeholder is only once in the string, thus recalculation should not be a problem.
                    case "player":
                        result.append(PlayerPlaceholders.PLAYER.getReplacement(player));
                        break;
                    case "ping":
                        result.append(PlayerPlaceholders.PING.getReplacement(player));
                        break;
                    case "team":
                        result.append(TeamPlaceholders.TEAM.getReplacement(players));
                        break;
                    case "world":
                        result.append(WorldPlaceholders.WORLD.getReplacement(world));
                        break;
                    case "date":
                        result.append(SimplePlaceholders.DATE.getReplacement());
                        break;
                    case "time":
                        result.append(SimplePlaceholders.TIME.getReplacement());
                        break;
                    case "server":
                        result.append(SimplePlaceholders.SERVER.getReplacement());
                        break;
                    case "tps":
                        result.append(SimplePlaceholders.TPS.getReplacement());
                        break;
                    default:
                        // Otherwise add the placeholder as plain-string
                        result.append('{').append(placeholderBuilder).append('}');
                        break;
                }

                // Make sure the '}' char is not recorded.
                continue;
            }

            // Record any char in the correct builder.
            if (placeholderStarted) {
                placeholderBuilder.append(c);
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    @Getter
    public enum PlayerPlaceholders
    {
        // Single placeholder
        PLAYER(player -> StringUtil.limitStringLength(player.getName(), 30)),
        PING(player -> StringUtil.limitStringLength(String.valueOf(ServerUtil.getPing(player)), 5));

        private final Function<Player, String> function;

        PlayerPlaceholders(Function<Player, String> function)
        {
            this.function = function;
        }

        public String getReplacement(Player player)
        {
            return this.function.apply(player);
        }
    }

    @Getter
    public enum TeamPlaceholders
    {
        TEAM(players -> players.stream().distinct().map(Player::getName).collect(Collectors.joining(", ")));

        private final Function<Collection<Player>, String> function;

        TeamPlaceholders(Function<Collection<Player>, String> function)
        {
            this.function = function;
        }

        public String getReplacement(Collection<Player> players)
        {
            return this.function.apply(players);
        }
    }

    @Getter
    public enum WorldPlaceholders
    {
        // Team placeholders
        WORLD(World::getName);

        private final Function<World, String> function;

        WorldPlaceholders(Function<World, String> supplier)
        {
            this.function = supplier;
        }

        public String getReplacement(World world)
        {
            return this.function.apply(world);
        }
    }

    @Getter
    public enum SimplePlaceholders
    {
        // Global placeholders
        DATE(() -> LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)),
        TIME(() -> StringUtil.limitStringLength(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME), 8)),
        SERVER(() -> Bukkit.getServer().getName()),
        TPS(() -> StringUtil.limitStringLength(String.valueOf(ServerUtil.getTPS()), 5));

        private final Supplier<String> supplier;

        SimplePlaceholders(Supplier<String> supplier)
        {
            this.supplier = supplier;
        }

        public String getReplacement()
        {
            return this.supplier.get();
        }
    }
}

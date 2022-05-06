package de.photon.anticheataddition.util.execute;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.util.minecraft.ping.PingProvider;
import de.photon.anticheataddition.util.minecraft.tps.TPSProvider;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.function.Supplier;

@UtilityClass
public final class Placeholders
{
    /**
     * Applies a placeholder to a {@link String}.
     *
     * @param original the original {@link String} containing all placeholders
     *
     * @return original with the placeholder replaced.
     */
    public static String replacePlaceholders(String original, Player player)
    {
        Preconditions.checkNotNull(player, "Tried to replace placeholders without player.");

        val world = player.getWorld();

        val placeholderBuilder = new StringBuilder();
        val result = new StringBuilder();
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
            if (placeholderStarted) placeholderBuilder.append(c);
            else result.append(c);
        }

        return result.toString();
    }

    @RequiredArgsConstructor
    public enum PlayerPlaceholders
    {
        // Single placeholder
        PLAYER(player -> StringUtils.left(player.getName(), 30)),
        PING(player -> StringUtils.left(String.valueOf(PingProvider.INSTANCE.getPing(player)), 5));

        private final Function<Player, String> function;

        public String getReplacement(Player player)
        {
            return this.function.apply(player);
        }
    }

    @RequiredArgsConstructor
    public enum WorldPlaceholders
    {
        // Team placeholders
        // No method reference here due to changes in spigot's world handling!
        WORLD(world -> world.getName());

        private final Function<World, String> function;

        public String getReplacement(World world)
        {
            return this.function.apply(world);
        }
    }

    @RequiredArgsConstructor
    public enum SimplePlaceholders
    {
        // Global placeholders
        DATE(() -> LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)),
        TIME(() -> StringUtils.left(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME), 8)),
        SERVER(() -> Bukkit.getServer().getName()),
        TPS(() -> StringUtils.left(String.valueOf(TPSProvider.INSTANCE.getTPS()), 5));

        private final Supplier<String> supplier;

        public String getReplacement()
        {
            return this.supplier.get();
        }
    }
}

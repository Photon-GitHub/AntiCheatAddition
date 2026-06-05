package de.photon.anticheataddition.util.execute;

import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.minecraft.ping.PingProvider;
import de.photon.anticheataddition.util.minecraft.tps.TPSProvider;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.function.Supplier;

@UtilityClass
public final class Placeholders {
    /**
     * Applies a placeholder to a {@link String}.
     *
     * @param original the original {@link String} containing all placeholders
     * @return original with the placeholder replaced.
     */
    public static String replacePlaceholders(String original, @Nullable Player player)
    {
        if (player == null) Log.finer(() -> "Placeholder replacement with null player: " + original);

        final var world = player == null ? null : player.getWorld();

        final var placeholderBuilder = new StringBuilder();
        final var result = new StringBuilder();
        boolean placeholderStarted = false;

        for (char c : original.toCharArray()) {
            if (c == '{') {
                // Clear any old placeholder that is still present.
                placeholderBuilder.delete(0, placeholderBuilder.length());
                // Start the recording of the placeholder
                placeholderStarted = true;
                // Make sure the '{' char is not recorded.
                continue;
            } else if (c == '}') {
                // End the recording of the placeholder
                placeholderStarted = false;

                // See if the recorded chars match a placeholder
                switch (placeholderBuilder.toString()) {
                    // Usually one placeholder is only once in the string, thus recalculation should not be a problem.
                    case "player" -> result.append(PlayerPlaceholders.PLAYER.getReplacement(player));
                    case "ping" -> result.append(PlayerPlaceholders.PING.getReplacement(player));
                    case "world" -> result.append(WorldPlaceholders.WORLD.getReplacement(world));
                    case "date" -> result.append(SimplePlaceholders.DATE.getReplacement());
                    case "time" -> result.append(SimplePlaceholders.TIME.getReplacement());
                    case "server" -> result.append(SimplePlaceholders.SERVER.getReplacement());
                    case "tps" -> result.append(SimplePlaceholders.TPS.getReplacement());
                    // Otherwise, add the placeholder as plain-string
                    default -> result.append('{').append(placeholderBuilder).append('}');
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
    public enum PlayerPlaceholders {
        // Single placeholder
        PLAYER(player -> limitChars(player.getName(), 30)),
        PING(player -> limitChars(String.valueOf(PingProvider.INSTANCE.getPing(player)), 5));

        private final Function<Player, String> function;

        public String getReplacement(@Nullable Player player)
        {
            if (player == null) return "";
            return this.function.apply(player);
        }
    }

    @RequiredArgsConstructor
    public enum WorldPlaceholders {
        // Team placeholders
        // No method reference here due to changes in spigot's world handling!
        @SuppressWarnings("Convert2MethodRef") WORLD(world -> world.getName());

        private final Function<World, String> function;

        public String getReplacement(@Nullable World world)
        {
            if (world == null) return "";
            return this.function.apply(world);
        }
    }

    @RequiredArgsConstructor
    public enum SimplePlaceholders {
        // Global placeholders
        DATE(() -> LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE)),
        TIME(() -> limitChars(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME), 8)),
        SERVER(() -> Bukkit.getServer().getName()),
        TPS(() -> limitChars(String.valueOf(TPSProvider.INSTANCE.getTPS()), 5));

        private final Supplier<String> supplier;

        public String getReplacement()
        {
            return this.supplier.get();
        }
    }

    private static String limitChars(String str, int limit)
    {
        if (str.length() <= limit) return str;
        return str.substring(0, limit);
    }
}

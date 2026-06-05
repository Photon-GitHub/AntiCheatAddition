package de.photon.anticheataddition.util.execute;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.util.minecraft.ping.PingProvider;
import de.photon.anticheataddition.util.minecraft.tps.TPSProvider;
import lombok.RequiredArgsConstructor;
import lombok.experimental.UtilityClass;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import java.util.function.Supplier;

@UtilityClass
public final class Placeholders {


    public static String replacePlaceholdersSafely(@NotNull String original)
    {
        Preconditions.checkNotNull(original, "Tried to process placeholders null string.");
        return processPlaceholders(original, null, null);
    }

    private static String safeReplacementFunction(@NotNull String placeholder)
    {
        return switch (placeholder) {
            // Usually one placeholder is only once in the string, thus recalculation should not be a problem.
            case "date" -> SimplePlaceholders.DATE.getReplacement();
            case "time" -> SimplePlaceholders.TIME.getReplacement();
            case "server" -> SimplePlaceholders.SERVER.getReplacement();
            case "tps" -> SimplePlaceholders.TPS.getReplacement();
            // Otherwise, add the placeholder as plain-string
            default -> '{' + placeholder + '}';
        };
    }

    /**
     * Applies a placeholder to a {@link String}.
     *
     * @param original the original {@link String} containing all placeholders
     * @return original with the placeholder replaced.
     */
    public static String replacePlaceholders(@NotNull String original, @NotNull Player player)
    {
        Preconditions.checkNotNull(original, "Tried to process placeholders null string.");
        Preconditions.checkNotNull(player, "Tried to replace player placeholders with null player.");
        return processPlaceholders(original, player, player.getWorld());
    }

    private static String nonNullPlayerReplacementFunction(@NotNull String placeholder, @NotNull Player player, @NotNull World world)
    {
        return switch (placeholder) {
            // Usually one placeholder is only once in the string, thus recalculation should not be a problem.
            case "player" -> PlayerPlaceholders.PLAYER.getReplacement(player);
            case "ping" -> PlayerPlaceholders.PING.getReplacement(player);
            case "world" -> WorldPlaceholders.WORLD.getReplacement(world);
            case "date" -> SimplePlaceholders.DATE.getReplacement();
            case "time" -> SimplePlaceholders.TIME.getReplacement();
            case "server" -> SimplePlaceholders.SERVER.getReplacement();
            case "tps" -> SimplePlaceholders.TPS.getReplacement();
            // Otherwise, add the placeholder as plain-string
            default -> '{' + placeholder + '}';
        };
    }

    private static String processPlaceholders(@NotNull String original, @Nullable Player player, World world)
    {
        final boolean safeReplacement = player == null;

        int placeholderStart = original.indexOf('{');
        // Fast check if there are any placeholders present in the string.
        if (placeholderStart == -1) return original;

        final var result = new StringBuilder(original.length());
        int lastAppendEnd = 0;

        while (placeholderStart != -1) {
            final int placeholderEnd = original.indexOf('}', placeholderStart + 1);
            if (placeholderEnd == -1) break;

            // Append everything before the placeholder.
            result.append(original, lastAppendEnd, placeholderStart);

            // Append the placeholder (substring without the brackets).
            final String placeholder = original.substring(placeholderStart + 1, placeholderEnd);
            if (safeReplacement) result.append(safeReplacementFunction(placeholder));
            else result.append(nonNullPlayerReplacementFunction(placeholder, player, world));

            // Compute the next iteration's start.'
            lastAppendEnd = placeholderEnd + 1;
            placeholderStart = original.indexOf('{', lastAppendEnd);
        }

        return result.append(original, lastAppendEnd, original.length()).toString();
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

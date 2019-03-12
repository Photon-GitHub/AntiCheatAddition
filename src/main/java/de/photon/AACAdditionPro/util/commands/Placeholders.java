package de.photon.AACAdditionPro.util.commands;

import com.google.common.base.Preconditions;
import de.photon.AACAdditionPro.util.general.StringUtils;
import de.photon.AACAdditionPro.util.server.ServerUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public final class Placeholders
{
    /**
     * This method handles the replacement of the placeholders.
     * Supported placeholders: {player}, {ping}, {tps}
     *
     * @param input  the input {@link String} with the placeholders that should be replaced.
     * @param player the {@link Player} who gets the violation
     *
     * @return the final {@link String} with the actual replacements in the place of the placeholders.
     */
    public static String applyPlaceholders(String input, final Player player, final String violationInformation)
    {
        Preconditions.checkNotNull(player, "Placeholder-parsing failed because the list of players is null or empty.");

        // Player
        input = applySinglePlaceholder(input, "{player}", player.getName(), (byte) 32);

        // Ping
        input = applySinglePlaceholder(input, "{ping}", String.valueOf(ServerUtil.getPing(player)), (byte) 5);

        // Global placeholders
        return applyGlobalPlaceholders(input, player.getWorld(), violationInformation);
    }

    /**
     * This method handles the replacement of the placeholders.
     * Supported placeholders: {team}, {tps}
     *
     * @param input   the input {@link String} with the placeholders that should be replaced.
     * @param players the {@link Player}s who get the violation
     *
     * @return the final {@link String} with the actual replacements in the place of the placeholders.
     */
    public static String applyPlaceholders(String input, final List<Player> players, final String violationInformation)
    {
        Preconditions.checkArgument(players != null && players.size() > 1, "Placeholder-parsing failed because the list of players is null or too small.");

        // Collect to Set to make sure no names are duplicates.
        input = applySinglePlaceholder(input, "{team}", String.join(", ", players.stream().map(Player::getName).collect(Collectors.toSet())), Byte.MAX_VALUE);

        return applyGlobalPlaceholders(input, players.get(0).getWorld(), violationInformation);
    }

    /**
     * Apply the placeholders which are important for both team and single player violations.
     */
    private static String applyGlobalPlaceholders(String input, final World world, final String violationInformation)
    {
        final LocalDateTime now = LocalDateTime.now();

        // Date
        input = applySinglePlaceholder(input, "{date}", now.format(DateTimeFormatter.ISO_LOCAL_DATE), Byte.MAX_VALUE);

        // Time
        input = applySinglePlaceholder(input, "{time}", now.format(DateTimeFormatter.ISO_LOCAL_TIME), (byte) 8);

        // Server name
        input = applySinglePlaceholder(input, "{server}", Bukkit.getServerName(), Byte.MAX_VALUE);

        // Ticks per second
        input = applySinglePlaceholder(input, "{tps}", String.valueOf(ServerUtil.getTPS()), (byte) 5);

        if (violationInformation != null) {
            input = applySinglePlaceholder(input, "{vl}", violationInformation, (byte) 5);
        }

        // World
        input = applySinglePlaceholder(input, "{world}", world.getName(), Byte.MAX_VALUE);
        return input;
    }

    /**
     * Applies a placeholder to a {@link String}.
     *
     * @param original     the original {@link String} containing all placeholders
     * @param placeholder  the placeholder pattern
     * @param replacement  the {@link String} the placeholder should be replaced with
     * @param maximumChars after how many chars should the replacement be chopped down
     *
     * @return original with the placeholder replaced.
     */
    private static String applySinglePlaceholder(String original, String placeholder, String replacement, byte maximumChars)
    {
        return original.replace(placeholder, StringUtils.limitStringLength(replacement, maximumChars));
    }
}
package de.photon.aacadditionpro.util.commands;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import de.photon.aacadditionpro.util.general.StringUtils;
import de.photon.aacadditionpro.util.server.ServerUtil;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class Placeholders
{
    private static final Map<String, Placeholder> PLACEHOLDER_MAP;

    static {
        final ImmutableMap.Builder<String, Placeholder> builder = ImmutableMap.builder();

        for (Placeholder placeholder : Placeholder.values()) {
            builder.put(placeholder.getPlaceholderString(), placeholder);
        }

        PLACEHOLDER_MAP = builder.build();
    }

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
        final String[] replacements = new String[PLACEHOLDER_MAP.size()];

        // Player
        replacements[Placeholder.PLAYER.ordinal()] = player.getName();

        // Ping
        replacements[Placeholder.PING.ordinal()] = String.valueOf(ServerUtil.getPing(player));

        // Global placeholders
        return applyGlobalPlaceholders(input, replacements, player.getWorld(), violationInformation);
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
        final String[] replacements = new String[PLACEHOLDER_MAP.size()];

        // Collect to Set to make sure no names are duplicates.
        replacements[Placeholder.TEAM.ordinal()] = String.join(", ", players.stream().map(Player::getName).collect(Collectors.toSet()));

        return applyGlobalPlaceholders(input, replacements, players.get(0).getWorld(), violationInformation);
    }

    /**
     * Apply the placeholders which are important for both team and single player violations.
     */
    private static String applyGlobalPlaceholders(String input, String[] replacements, final World world, final String violationInformation)
    {
        final LocalDateTime now = LocalDateTime.now();

        // Date
        replacements[Placeholder.DATE.ordinal()] = now.format(DateTimeFormatter.ISO_LOCAL_DATE);

        // Time
        replacements[Placeholder.TIME.ordinal()] = now.format(DateTimeFormatter.ISO_LOCAL_TIME);

        // Server name
        replacements[Placeholder.SERVER.ordinal()] = Bukkit.getServer().getName();

        // Ticks per second
        replacements[Placeholder.TPS.ordinal()] = String.valueOf(ServerUtil.getTPS());

        // VL extra information
        replacements[Placeholder.VL.ordinal()] = violationInformation;

        // World
        replacements[Placeholder.WORLD.ordinal()] = world.getName();
        return replacePlaceholders(input, replacements);
    }

    /**
     * Applies a placeholder to a {@link String}.
     *
     * @param original the original {@link String} containing all placeholders
     *
     * @return original with the placeholder replaced.
     */
    private static String replacePlaceholders(String original, String[] replacements)
    {
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
                final Placeholder placeholder = PLACEHOLDER_MAP.get(placeholderBuilder.toString());

                // If so, add the placeholder to the result
                if (placeholder != null) {
                    result.append(StringUtils.limitStringLength(replacements[placeholder.ordinal()], placeholder.maximumChars));
                }
                // Otherwise add the placeholder as plain-string
                else {
                    result.append('{').append(placeholderBuilder.toString()).append('}');
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

    @RequiredArgsConstructor
    private enum Placeholder
    {
        // Single placeholder
        PLAYER((byte) 32),
        PING((byte) 5),
        // Team placeholders
        TEAM(Byte.MAX_VALUE),
        // Global placeholders
        DATE(Byte.MAX_VALUE),
        TIME((byte) 8),
        SERVER(Byte.MAX_VALUE),
        TPS((byte) 5),
        VL((byte) 5),
        WORLD(Byte.MAX_VALUE);

        private final byte maximumChars;

        public String getPlaceholderString()
        {
            return this.name().toLowerCase();
        }
    }
}
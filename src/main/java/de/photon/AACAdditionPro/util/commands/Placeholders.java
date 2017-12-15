package de.photon.AACAdditionPro.util.commands;

import me.konsolas.aac.api.AACAPIProvider;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
    public static String applyPlaceholders(final String input, final Player player)
    {
        return applyPlaceholders(input, Collections.singletonList(player));
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
    public static String applyPlaceholders(String input, final List<Player> players)
    {
        if (AACAPIProvider.isAPILoaded())
        {
            // List is not null and contains at least one player
            if (players != null && !players.isEmpty())
            {
                // Team handling
                if (players.size() > 1)
                {

                    // Team
                    final StringBuilder teamString = new StringBuilder();

                    Iterator<Player> playerIterator = players.iterator();
                    Player player;
                    while (true)
                    {
                        player = playerIterator.next();
                        teamString.append(player.getName());

                        if (playerIterator.hasNext())
                        {
                            teamString.append(", ");
                        }
                        else
                        {
                            break;
                        }
                    }

                    input = applySinglePlaceholder(input, "{team}", teamString.toString(), Byte.MAX_VALUE);
                    // Single-Player handling
                }
                else
                {

                    // Player
                    input = applySinglePlaceholder(input, "{player}", players.get(0).getName(), (byte) 32);

                    // Ping
                    input = applySinglePlaceholder(input, "{ping}", String.valueOf(AACAPIProvider.getAPI().getPing(players.get(0))), (byte) 5);
                }

                // Both team and single player need the following placeholders
                input = applySinglePlaceholder(input, "{tps}", String.valueOf(AACAPIProvider.getAPI().getTPS()), (byte) 5);

                // World
                input = applySinglePlaceholder(input, "{world}", players.get(0).getWorld().getName(), Byte.MAX_VALUE);
                return input;
            }
            throw new NullPointerException("Placeholder-parsing failed because the list of players is null or empty.");
        }
        throw new IllegalStateException("Placeholder-parsing failed because AAC's API is not loaded.");
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
        // No need to reduce replacement.lengh() by 1 as substring's last letter handling is exclusive.
        return original.replace(placeholder, replacement.substring(0, Math.min(replacement.length(), maximumChars)));
    }
}
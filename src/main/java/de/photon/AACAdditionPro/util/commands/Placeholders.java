package de.photon.AACAdditionPro.util.commands;

import me.konsolas.aac.api.AACAPIProvider;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Placeholders
{
    private static final Map<String[], Byte> placeholders = new HashMap<>(4, 1);

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
        if (AACAPIProvider.isAPILoaded()) {
            // List is not null and contains at least one player
            if (players != null && !players.isEmpty()) {

                // Determine whether it needs to handle a team
                // Don't forget to increase the initialCapacity of the HashMap if you add new placeholders
                // The number at the end represents the maximum length of the variable

                if (players.size() > 1) {

                    // Team
                    final StringBuilder teamString = new StringBuilder();
                    for (final Player player : players) {
                        teamString.append(player.getName());
                        teamString.append(", ");
                    }

                    placeholders.put(new String[]{
                            "{team}",
                            // Remove the last ", "
                            // Exclusive ending !
                            teamString.toString().substring(0, teamString.length() - 2)
                    }, Byte.MAX_VALUE);
                } else {

                    // Player
                    placeholders.put(new String[]{
                            "{player}",
                            players.get(0).getName()
                    }, (byte) 32);

                    // Ping
                    placeholders.put(new String[]{
                            "{ping}",
                            String.valueOf(AACAPIProvider.getAPI().getPing(players.get(0)))
                    }, (byte) 5);
                }

                // Both team and single player need the following placeholders

                // TPS
                placeholders.put(new String[]{
                        "{tps}",
                        String.valueOf(AACAPIProvider.getAPI().getTPS())
                }, (byte) 5);

                // World
                placeholders.put(new String[]{
                        "{world}",
                        players.get(0).getWorld().getName()
                }, Byte.MAX_VALUE);

                //Go through all the placeholders
                for (final Map.Entry<String[], Byte> entry : placeholders.entrySet()) {
                    String replaceString = entry.getKey()[1];

                    // Make sure that the length is not too big
                    if (replaceString.length() > entry.getValue()) {
                        replaceString = replaceString.substring(0, entry.getValue());
                    }

                    input = input.replace(entry.getKey()[0], replaceString);
                }
                return input;
            }
            throw new RuntimeException("Player is null.");
        }
        throw new RuntimeException("The placeholder-parsing failed as AAC's API is not loaded.");
    }
}
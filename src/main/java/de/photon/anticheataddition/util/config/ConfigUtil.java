package de.photon.anticheataddition.util.config;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.AntiCheatAddition;
import lombok.experimental.UtilityClass;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

@UtilityClass
public final class ConfigUtil
{
    /**
     * Used to load a {@link List} of {@link String}s if it is uncertain if the value of path
     * is a {@link String} or a {@link List} of {@link String}s
     *
     * @param path the path which should be loaded
     *
     * @return an {@link List} of {@link String}s with the path as entries.
     */
    @NotNull
    public static List<String> loadImmutableStringOrStringList(@NotNull final String path)
    {
        final List<String> input = AntiCheatAddition.getInstance().getConfig().getStringList(path);

        if (input.isEmpty() && AntiCheatAddition.getInstance().getConfig().isString(path)) {
            final String possibleString = AntiCheatAddition.getInstance().getConfig().getString(path);

            if (possibleString == null) return List.of();

            return switch (possibleString) {
                case "", "{}", "{ }", "[]", "[ ]" -> List.of();
                default -> List.of(possibleString);
            };
        }
        return input;
    }

    /**
     * Tries to load all keys from a path in the config.
     *
     * @param sectionPath the given path to the section which keys should be loaded.
     *
     * @return a {@link Set} of {@link String}s that represent the keys
     *
     * @throws NullPointerException in the case that the loaded {@link ConfigurationSection} is null.
     */
    public static Set<String> loadKeys(final String sectionPath)
    {
        // Return all the keys of the provided section.
        return Preconditions.checkNotNull(AntiCheatAddition.getInstance().getConfig().getConfigurationSection(sectionPath),
                                          "Config loading error: ConfigurationSection does not exist at " + sectionPath).getKeys(false);
    }

    /**
     * Determines whether a string (config line) is a comment in a YAML-config.
     */
    public static boolean isConfigComment(final String string)
    {
        // If the first letter after whitespaces is not # assume that some data/key comes first.
        return string == null || string.isEmpty() || string.stripLeading().charAt(0) == '#';
    }

    /**
     * Counts the leading whitespaces of a {@link String}
     *
     * @return the amount of leading whitespaces.
     */
    public static long depth(final String string)
    {
        return string == null || string.isEmpty() ? 0 : string.chars().takeWhile(Character::isWhitespace).count();
    }
}

package de.photon.aacadditionpro.util.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.val;

import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringUtil
{
    /**
     * Determines whether a string (config line) is a comment in a YAML-config.
     */
    public static boolean isConfigComment(final String string)
    {
        // If the first letter after whitespaces is not # assume that some data/key comes first.
        return string == null || string.isEmpty() || string.strip().indexOf('#') == 0;
    }

    /**
     * Counts the leading whitespaces of a {@link String}
     *
     * @return the amount of leading whitespaces. 0 if there are only whitespaces present.
     */
    public static int depth(final String string)
    {
        for (int i = 0, n = string.length(); i < n; ++i) {
            if (string.charAt(i) != ' ') return i;
        }
        return 0;
    }

    /**
     * Determines whether one or more {@link String}s are included in another {@link String}.
     *
     * @param input the base {@link String} which is checked for the other {@link String}s.
     * @param flags the other {@link String}s which should be searched for in the base {@link String}.
     */
    public static boolean stringContainsAnyFlag(final String input, final String... flags)
    {
        for (String flag : flags) {
            if (input.contains(flag)) return true;
        }
        return false;
    }

    /**
     * Determines whether one or more {@link String}s are included in another {@link String} whilst ignoring the case of the chars.
     *
     * @param input the base {@link String} which is checked for the other {@link String}s.
     * @param flags the other {@link String}s which should be searched for in the base {@link String}.
     */
    public static boolean stringContainsFlagsIgnoreCase(final String input, final String... flags)
    {
        val lowerCaseInput = input.toLowerCase(Locale.ENGLISH);
        for (String flag : flags) {
            if (lowerCaseInput.contains(flag.toLowerCase(Locale.ENGLISH))) return true;
        }
        return false;
    }
}

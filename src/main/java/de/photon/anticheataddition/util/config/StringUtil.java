package de.photon.anticheataddition.util.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

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
}

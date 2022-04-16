package de.photon.anticheataddition.util.config;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class StringUtil
{
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

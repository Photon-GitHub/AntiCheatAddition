package de.photon.aacadditionpro.util.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringUtil
{
    /**
     * Constructs a new {@link String} from a byte array according to the {@link StandardCharsets#UTF_8}.
     * This is used in various plugin messages.
     */
    public static String fromUTF8Bytes(final byte[] bytes)
    {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Limits the length of a {@link String} to a certain value.
     * If the length of the {@link String} is already small enough the {@link String} will be returned without modification.
     *
     * @param input        the {@link String} which should be limited
     * @param maximumChars the maximum allowed chars
     */
    public static String limitStringLength(final String input, int maximumChars)
    {
        // No need to reduce input.length() by 1 as substring's last letter handling is exclusive.
        return input.substring(0, Math.min(input.length(), maximumChars));
    }

    /**
     * Counts the leading whitespaces of a {@link String}
     *
     * @return the amount of leading whitespaces. 0 if there are only whitespaces present.
     */
    public static int depth(final String string)
    {
        for (int i = 0; i < string.length(); ++i) {
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
        final String lowerCaseInput = input.toLowerCase(Locale.ENGLISH);
        for (String flag : flags) {
            if (lowerCaseInput.contains(flag.toLowerCase(Locale.ENGLISH))) return true;
        }
        return false;
    }
}

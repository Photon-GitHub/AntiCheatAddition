package de.photon.AACAdditionPro.util.general;

public final class StringUtils
{
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
}

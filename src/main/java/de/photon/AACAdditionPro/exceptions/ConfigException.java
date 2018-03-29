package de.photon.AACAdditionPro.exceptions;

/**
 * An exception to get all exceptions occurred in connection with the config together.
 * Use {@link Exception#getCause()} to get the cause of this exception.
 *
 * @author Jan Marian Meyer
 */
public final class ConfigException extends Exception
{

    /**
     * @param cause the occurred error
     */
    public ConfigException(final Throwable cause)
    {
        super("An error has occurred in connection with the configuration", cause instanceof ConfigException ?
                                                                            cause.getCause() :
                                                                            cause);
    }
}
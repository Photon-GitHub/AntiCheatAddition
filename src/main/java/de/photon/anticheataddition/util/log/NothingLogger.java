package de.photon.anticheataddition.util.log;

import lombok.experimental.UtilityClass;

import java.util.logging.Level;
import java.util.logging.Logger;

@UtilityClass
public final class NothingLogger
{
    public static final Logger INSTANCE;

    static {
        INSTANCE = Logger.getLogger(NothingLogger.class.getName());

        // Set the logging level to OFF
        INSTANCE.setLevel(Level.OFF);
    }
}

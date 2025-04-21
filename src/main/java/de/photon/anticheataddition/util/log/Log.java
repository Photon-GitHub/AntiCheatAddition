package de.photon.anticheataddition.util.log;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.AntiCheatAddition;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public final class Log
{
    public static final Log INSTANCE = new Log();

    private static final Level CONSOLE_LEVEL = getConfigLevel("Debug.console_level");
    private static final Level FILE_LEVEL = getConfigLevel("Debug.file_level");
    private static final Level PLAYER_LEVEL = getConfigLevel("Debug.player_level");

    private static Level getConfigLevel(String path)
    {
        final String value = AntiCheatAddition.getInstance().getConfig().getString(path);
        Preconditions.checkNotNull(value, "Debug level setting is not present in config. Please regenerate your config.");
        return Level.parse(value);
    }

    public static void log(Level level, String message, Exception e)
    {
        INSTANCE.logger.log(level, message, e);
    }

    public static void log(Level level, Supplier<String> message)
    {
        INSTANCE.logger.log(level, message);
    }

    public static void finest(Supplier<String> message)
    {
        INSTANCE.logger.finest(message);
    }

    public static void finer(Supplier<String> message)
    {
        INSTANCE.logger.finer(message);
    }

    public static void fine(Supplier<String> message)
    {
        INSTANCE.logger.fine(message);
    }

    public static void info(Supplier<String> message)
    {
        INSTANCE.logger.info(message);
    }

    public static void warning(Supplier<String> message)
    {
        INSTANCE.logger.warning(message);
    }

    public static void severe(Supplier<String> message)
    {
        INSTANCE.logger.severe(message);
    }

    public static void error(String message, Throwable thrown)
    {
        INSTANCE.logger.log(Level.SEVERE, message, thrown);
    }

    public Logger logger;
    private final LogFileHandler logFileHandler;

    public Log()
    {
        // By default, the logger is set to do nothing to prevent testing errors.
        this.logger = NothingLogger.INSTANCE;
        this.logFileHandler = new LogFileHandler(logger);
    }

    public void bukkitSetup()
    {
        this.logger = AntiCheatAddition.getInstance().getLogger();

        if (!Level.OFF.equals(CONSOLE_LEVEL)) INSTANCE.logger.addHandler(new ConsoleLogHandler(CONSOLE_LEVEL));
        if (!Level.OFF.equals(FILE_LEVEL)) logFileHandler.startCycle(this.logger, FILE_LEVEL);
        if (!Level.OFF.equals(PLAYER_LEVEL)) INSTANCE.logger.addHandler(new DebugUserLogHandler(PLAYER_LEVEL));

        // Set the smallest level as the main logger (smaller -> more messages) to ensure all handlers get their messages.
        INSTANCE.logger.setLevel(minLevel(CONSOLE_LEVEL, FILE_LEVEL, PLAYER_LEVEL));

        // Add the violation debug messages.
        AntiCheatAddition.getInstance().registerListener(new ViolationLogger());

        fine(() -> "Logger handlers: " + Arrays.stream(INSTANCE.logger.getHandlers()).map(handler -> handler.getClass().getName() + " with level " + handler.getLevel()).collect(Collectors.joining(", ")));
        info(() -> "Logging setup finished. Console: " + CONSOLE_LEVEL.getName() + " | File: " + FILE_LEVEL.getName() + " | Player: " + PLAYER_LEVEL.getName());
    }

    public static Level minLevel(Level... levels)
    {
        Level min = Level.ALL;
        for (Level level : levels) {
            if (level.intValue() < min.intValue()) min = level;
        }
        return min;
    }

    public void close()
    {
        this.logFileHandler.close();
    }
}

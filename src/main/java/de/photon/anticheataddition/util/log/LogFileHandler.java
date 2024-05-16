package de.photon.anticheataddition.util.log;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.util.mathematics.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

/**
 * The LogFileHandler class manages log file creation and rotation for AntiCheatAddition.
 * It creates new log files daily and ensures log messages are appropriately formatted and written.
 */
public final class LogFileHandler
{

    // Constants for log message formatting
    private static final String REPLACE_PREFIX = ChatColor.stripColor(AntiCheatAddition.ANTICHEAT_ADDITION_PREFIX);
    private static final DateTimeFormatter PREFIX_TIME_FORMATTER = DateTimeFormatter.ofPattern("'['HH:mm:ss.SSS']' ");

    private static final Formatter LOG_FILE_FORMATTER = new Formatter()
    {
        @Override
        public String format(LogRecord logRecord)
        {
            return LocalDateTime.now().format(PREFIX_TIME_FORMATTER) +
                   // Do not use simple substring here as purpur uses a different format.
                   ChatColor.stripColor(formatMessage(logRecord)).replace(REPLACE_PREFIX, "") + System.lineSeparator();
        }
    };

    private Logger logger;
    private Level fileLevel = Level.OFF;
    private FileHandler currentHandler;

    /**
     * Constructs a new LogFileHandler.
     *
     * @param logger the Logger instance to which this handler is attached
     */
    public LogFileHandler(@NotNull Logger logger)
    {
        this.logger = Preconditions.checkNotNull(logger, "Logger cannot be null.");
    }

    public void startCycle(Logger logger, Level level)
    {
        this.logger = logger;
        this.fileLevel = level;

        // 5 seconds after the next day to prevent date problems.
        final var nextDay = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(0, 0, 5));
        final long difference = LocalDateTime.now().until(nextDay, ChronoUnit.MILLIS);
        // Schedule the first log file replacement to occur at the start of the next day. Then have a daily cycle.
        this.replaceDebugFile();
        Bukkit.getScheduler().runTaskTimer(AntiCheatAddition.getInstance(), this::replaceDebugFile, TimeUtil.toTicks(difference), TimeUtil.toTicks(1, TimeUnit.DAYS));
    }

    /**
     * Replaces the current log file with a new one, creating a new log file for the current day.
     * This method schedules itself to run again at the start of the next day.
     */
    private void replaceDebugFile()
    {
        final var now = LocalDateTime.now();
        final var oldHandler = currentHandler;

        // Create new handler.
        final String path = AntiCheatAddition.getInstance().getDataFolder().getPath() + File.separatorChar + "logs" + File.separatorChar + now.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log";
        final File file = new File(path);
        try {
            file.getParentFile().mkdirs();
            this.currentHandler = new FileHandler(path, true);
            this.currentHandler.setLevel(fileLevel);
            this.currentHandler.setFormatter(LOG_FILE_FORMATTER);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "AntiCheatAddition unable to create log file handler.", e);
        }

        // Replace old handler.
        this.logger.addHandler(currentHandler);
        if (oldHandler != null) this.logger.removeHandler(oldHandler);
    }

    /**
     * Closes the current log file handler, if one exists.
     */
    public void close()
    {
        if (currentHandler != null) currentHandler.close();
    }
}

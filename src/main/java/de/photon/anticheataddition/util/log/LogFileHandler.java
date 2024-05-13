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
import java.util.logging.*;

/**
 * The LogFileHandler class manages log file creation and rotation for AntiCheatAddition.
 * It creates new log files daily and ensures log messages are appropriately formatted and written.
 */
public class LogFileHandler
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

    private final Logger logger;
    private final Level fileLevel;
    private FileHandler currentHandler;

    /**
     * Constructs a new LogFileHandler.
     *
     * @param logger    the Logger instance to which this handler is attached
     * @param fileLevel the logging level for the log file
     */
    public LogFileHandler(@NotNull Logger logger, @NotNull Level fileLevel)
    {
        this.logger = Preconditions.checkNotNull(logger, "Logger cannot be null.");
        this.fileLevel = Preconditions.checkNotNull(fileLevel, "File level cannot be null.");
    }

    /**
     * Replaces the current log file with a new one, creating a new log file for the current day.
     * This method schedules itself to run again at the start of the next day.
     */
    public void replaceDebugFileCycle()
    {
        final var now = LocalDateTime.now();
        final var oldHandler = currentHandler;

        // Create new handler.
        final String path = AntiCheatAddition.getInstance().getDataFolder().getPath() + File.separatorChar + "logs" + File.separatorChar + now.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log";
        final File file = new File(path);
        try {
            file.getParentFile().mkdirs();
            this.currentHandler = new FileHandler(path, true);
            this.currentHandler.setLevel(this.fileLevel);
            this.currentHandler.setFormatter(LOG_FILE_FORMATTER);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, "AntiCheatAddition unable to create log file handler.", e);
        }

        // Replace old handler.
        this.logger.addHandler(currentHandler);
        if (oldHandler != null) this.logger.removeHandler(oldHandler);

        // Replace again the next day.
        // 5 seconds after the next day to prevent date problems.
        final var nextDay = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(0, 0, 5));
        final long difference = LocalDateTime.now().until(nextDay, ChronoUnit.MILLIS);
        Bukkit.getScheduler().scheduleSyncDelayedTask(AntiCheatAddition.getInstance(), this::replaceDebugFileCycle, TimeUtil.toMillis(difference));
    }

    /**
     * Closes the current log file handler, if one exists.
     */
    public void close()
    {
        if (currentHandler != null) currentHandler.close();
    }
}

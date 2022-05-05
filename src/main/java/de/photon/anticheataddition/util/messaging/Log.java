package de.photon.anticheataddition.util.messaging;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.util.mathematics.TimeUtil;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class Log
{
    public static final Log INSTANCE = new Log();
    // The prefix is always "[plugin name] ", so plugin name length + 3.
    private static final int PREFIX_CHARS = AntiCheatAddition.getInstance().getName().length() + 3;

    private static final DateTimeFormatter PREFIX_TIME_FORMATTER = DateTimeFormatter.ofPattern("'['HH:mm:ss.SSS']' ");

    private static final Formatter LOG_FILE_FORMATTER = new Formatter()
    {
        @Override
        public String format(LogRecord logRecord)
        {
            return LocalDateTime.now().format(PREFIX_TIME_FORMATTER) +
                   ChatColor.stripColor(formatMessage(logRecord)).substring(PREFIX_CHARS) +
                   System.lineSeparator();
        }
    };
    private static final Formatter DEBUG_USER_FORMATTER = new Formatter()
    {
        @Override
        public String format(LogRecord logRecord)
        {
            return formatMessage(logRecord);
        }
    };

    public static Logger logger()
    {
        return AntiCheatAddition.getInstance().getLogger();
    }

    public static void log(Level level, Supplier<String> message)
    {
        logger().log(level, message);
    }

    public static void finest(Supplier<String> message)
    {
        logger().finest(message);
    }

    public static void finer(Supplier<String> message)
    {
        logger().finer(message);
    }

    public static void fine(Supplier<String> message)
    {
        logger().fine(message);
    }

    public static void info(Supplier<String> message)
    {
        logger().info(message);
    }

    public static void warning(Supplier<String> message)
    {
        logger().warning(message);
    }

    public static void severe(Supplier<String> message)
    {
        logger().severe(message);
    }

    public static void error(String message, Throwable thrown)
    {
        logger().log(Level.SEVERE, message, thrown);
    }

    private final Level level = AntiCheatAddition.getInstance().getConfig().getBoolean("Debug.console") ? Level.FINE : Level.INFO;
    private FileHandler currentHandler = null;

    public void setup()
    {
        if (AntiCheatAddition.getInstance().getConfig().getBoolean("Debug.file")) {
            replaceDebugFileCycle();
        }

        if (AntiCheatAddition.getInstance().getConfig().getBoolean("Debug.players")) {
            logger().addHandler(new DebugUserHandler(level));
        }

        // Set the console level.
        logger().setLevel(level);

        // Add the violation debug messages.
        AntiCheatAddition.getInstance().registerListener(new ViolationLogger());
    }

    public void close()
    {
        if (currentHandler != null) currentHandler.close();
    }

    private void replaceDebugFileCycle()
    {
        val now = LocalDateTime.now();

        val oldHandler = currentHandler;

        // Create new handler.
        final String path = AntiCheatAddition.getInstance().getDataFolder().getPath() + File.separatorChar + "logs" + File.separatorChar + now.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log";
        try {
            this.currentHandler = new FileHandler(path, true);
            this.currentHandler.setLevel(level);
            this.currentHandler.setFormatter(LOG_FILE_FORMATTER);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.SEVERE, e, () -> "AntiCheatAddition unable to create log file handler ");
        }

        // Replace old handler.
        logger().addHandler(currentHandler);
        if (oldHandler != null) logger().removeHandler(oldHandler);

        // Replace again the next day.
        // 5 seconds after the next day to prevent date problems.
        val nextDay = LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.of(0, 0, 5));
        val difference = LocalDateTime.now().until(nextDay, ChronoUnit.MILLIS);
        Bukkit.getScheduler().scheduleSyncDelayedTask(AntiCheatAddition.getInstance(), this::replaceDebugFileCycle, TimeUtil.toMillis(difference));
    }

    private static class DebugUserHandler extends Handler
    {
        public DebugUserHandler(Level level)
        {
            this.setFormatter(DEBUG_USER_FORMATTER);
            this.setLevel(level);
        }

        @Override
        public void publish(LogRecord logRecord)
        {
            if (!isLoggable(logRecord)) return;

            val msg = getFormatter().format(logRecord);
            for (User debugUser : User.getDebugUsers()) debugUser.getPlayer().sendMessage(msg);
        }

        @Override
        public void flush()
        {
            // We do not buffer.
        }

        @Override
        public void close() throws SecurityException
        {
            // Not necessary for chat.
        }
    }
}

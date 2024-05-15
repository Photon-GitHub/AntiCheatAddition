package de.photon.anticheataddition.util.log;

import org.bukkit.Bukkit;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

final class ConsoleLogHandler extends Handler
{
    private static final Formatter CONSOLE_FORMATTER = new Formatter()
    {
        @Override
        public String format(LogRecord logRecord)
        {
            return formatMessage(logRecord);
        }
    };

    public ConsoleLogHandler(Level level)
    {
        this.setFormatter(CONSOLE_FORMATTER);
        this.setLevel(level);
    }

    @Override
    public void publish(LogRecord logRecord)
    {
        if (!isLoggable(logRecord)) return;
        // The default bukkit logger already logs INFO messages to console.
        if (logRecord.getLevel().intValue() >= Level.INFO.intValue()) return;

        final var msg = getFormatter().format(logRecord);
        Bukkit.getConsoleSender().sendMessage(msg);
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

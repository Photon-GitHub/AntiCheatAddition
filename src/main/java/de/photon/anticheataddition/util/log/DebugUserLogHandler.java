package de.photon.anticheataddition.util.log;

import de.photon.anticheataddition.user.User;

import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

final class DebugUserLogHandler extends Handler
{
    private static final Formatter DEBUG_USER_FORMATTER = new Formatter()
    {
        @Override
        public String format(LogRecord logRecord)
        {
            return formatMessage(logRecord);
        }
    };

    public DebugUserLogHandler(Level level)
    {
        this.setFormatter(DEBUG_USER_FORMATTER);
        this.setLevel(level);
    }

    @Override
    public void publish(LogRecord logRecord)
    {
        if (!isLoggable(logRecord)) return;

        final var msg = getFormatter().format(logRecord);
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

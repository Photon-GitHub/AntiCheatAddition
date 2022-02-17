package de.photon.aacadditionpro.util.messaging;

import lombok.val;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class LogFileHandler extends Handler
{
    private static final DateTimeFormatter PREFIX_TIME_FORMATTER = DateTimeFormatter.ofPattern("[HH:mm:ss.SSS] ");

    LogFile logFile;

    public LogFileHandler() throws IOException
    {
        this.setLevel(Level.ALL);
        logFile = new LogFile();
    }

    @Override
    public void publish(LogRecord logRecord)
    {
        // Check the level of the LogRecord.
        if (logRecord.getLevel().intValue() <= this.getLevel().intValue()) return;
        val message = logRecord.getMessage();
        val now = LocalDateTime.now();

        try {
            if (!this.logFile.isValid(now)) {
                this.close();
                this.logFile = new LogFile();
            }

            val writer = this.logFile.getWriter();
            // Write the prefix.
            writer.write(now.format(PREFIX_TIME_FORMATTER));
            // Write the message.
            writer.write(message);
            // Add a new line.
            writer.newLine();
            // Flush the entire message out.
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void flush()
    {
        try {
            this.logFile.getWriter().flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() throws SecurityException
    {
        this.flush();
        try {
            this.logFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.logFile = null;
    }
}

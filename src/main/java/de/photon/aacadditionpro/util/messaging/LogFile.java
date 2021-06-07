package de.photon.aacadditionpro.util.messaging;

import de.photon.aacadditionpro.AACAdditionPro;
import lombok.Value;
import lombok.val;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

@Value
public class LogFile
{
    File backingFile;
    int dayOfTheYear;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public LogFile(LocalDateTime now)
    {
        this.dayOfTheYear = now.getDayOfYear();
        File createdFile = null;
        try {
            createdFile = new File(AACAdditionPro.getInstance().getDataFolder().getPath() + "/logs/" + now.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log");
            createdFile.getParentFile().mkdirs();
            createdFile.createNewFile();
        } catch (IOException e) {
            AACAdditionPro.getInstance().getLogger().log(Level.SEVERE, "Something went wrong while trying to create the log file", e);
        }
        this.backingFile = createdFile;
    }

    public void write(String logMessage, LocalDateTime now)
    {
        // Reserve the required builder size.
        // Time length is always 12, together with 2 brackets and one space this will result in 15.
        val verboseMessage = new StringBuilder(15 + logMessage.length());
        // Add the beginning of the PREFIX
        verboseMessage.append('[');
        // Get the current time
        verboseMessage.append(now.format(DateTimeFormatter.ISO_LOCAL_TIME));

        // Add a 0 if it is too short
        // Technically only 12, but we already appended the "[", thus one more.
        while (verboseMessage.length() < 13) verboseMessage.append('0');

        // Add the rest of the PREFIX and the message
        verboseMessage.append(']').append(' ').append(logMessage).append('\n');

        try {
            // Log the message
            Files.write(this.backingFile.toPath(), verboseMessage.toString().getBytes(), StandardOpenOption.APPEND);
        } catch (final IOException e) {
            AACAdditionPro.getInstance().getLogger().log(Level.SEVERE, "Something went wrong while trying to write to the log file", e);
        }
    }

    public boolean isValid(LocalDateTime now)
    {
        val dayOfYear = now.getDayOfYear();
        return this.dayOfTheYear == dayOfYear && this.backingFile.exists();
    }
}

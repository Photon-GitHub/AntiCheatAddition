package de.photon.aacadditionpro.util.messaging;

import de.photon.aacadditionpro.AACAdditionPro;
import lombok.Value;
import lombok.val;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;

@Value
public class LogFile
{
    private static final DateTimeFormatter PREFIX_TIME_FORMATTER = DateTimeFormatter.ofPattern("[HH:mm:ss.SSS] ");

    File backingFile;
    int dayOfTheYear;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public LogFile(LocalDateTime now)
    {
        this.dayOfTheYear = now.getDayOfYear();
        val createdFile = new File(AACAdditionPro.getInstance().getDataFolder().getPath() + "/logs/" + now.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log");
        // Create parent folders here, but not the file itself as it is created on write (StandardOpenOption.CREATE)
        createdFile.getParentFile().mkdirs();
        this.backingFile = createdFile;
    }

    public void write(String logMessage, LocalDateTime now)
    {
        try {
            // Log the message
            Files.writeString(this.backingFile.toPath(), now.format(PREFIX_TIME_FORMATTER) + logMessage + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (final IOException e) {
            AACAdditionPro.getInstance().getLogger().log(Level.SEVERE, "Something went wrong while trying to write to the log file", e);
        }
    }

    /**
     * Check if the log file is still valid, i.e. it exists and the date is still the same.
     */
    public boolean isValid(LocalDateTime now)
    {
        return this.dayOfTheYear == now.getDayOfYear() && this.backingFile.exists();
    }
}

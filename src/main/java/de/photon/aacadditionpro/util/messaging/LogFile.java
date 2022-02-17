package de.photon.aacadditionpro.util.messaging;

import de.photon.aacadditionpro.AACAdditionPro;
import lombok.Value;
import lombok.val;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Value
public class LogFile implements AutoCloseable
{
    File backingFile;
    int dayOfTheYear;
    BufferedWriter writer;

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public LogFile() throws IOException
    {
        val now = LocalDateTime.now();
        this.dayOfTheYear = now.getDayOfYear();
        val createdFile = new File(AACAdditionPro.getInstance().getDataFolder().getPath() + "/logs/" + now.format(DateTimeFormatter.ISO_LOCAL_DATE) + ".log");
        // Create parent folders here, but not the file itself as it is created on write (StandardOpenOption.CREATE)
        createdFile.getParentFile().mkdirs();
        this.backingFile = createdFile;
        this.writer = Files.newBufferedWriter(this.backingFile.toPath(), StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.CREATE);
    }

    public boolean isValid(LocalDateTime now)
    {
        val dayOfYear = now.getDayOfYear();
        return this.dayOfTheYear == dayOfYear && this.backingFile.exists();
    }

    @Override
    public void close() throws IOException
    {
        this.writer.close();
    }
}

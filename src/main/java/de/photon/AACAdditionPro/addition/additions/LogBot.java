package de.photon.AACAdditionPro.addition.additions;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.addition.Addition;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LogBot implements Addition, Runnable {
    // HashMap's real capacity is always a power of 2
    private final Map<File, Long> logDeletionTimes = new HashMap<>(4, 1F);
    private int task_number;

    @Override
    public void run() {
        final long currentTime = System.currentTimeMillis();

        logDeletionTimes.forEach(
                (logFolder, timeToDelete) ->
                {
                    // If the logFolder exists
                    if (logFolder.exists()) {
                        final File[] files = logFolder.listFiles();

                        if (files != null) {
                            // The folder is not empty
                            for (final File file : files) {
                                final String nameOfFile = file.getName();
                                // Be sure it is a log file of AAC or AACAdditionPro (.log) or a log file of the server (.log.gz)
                                if ((nameOfFile.endsWith(".log") || nameOfFile.endsWith(".log.gz")) &&
                                    // Minimum time
                                    currentTime - file.lastModified() > timeToDelete) {
                                    if (file.delete()) {
                                        VerboseSender.sendVerboseMessage("Deleted " + nameOfFile);
                                    }
                                    else {
                                        VerboseSender.sendVerboseMessage("Could not delete old file " + nameOfFile, true, true);
                                    }
                                }
                            }
                        }
                    }
                    else {
                        VerboseSender.sendVerboseMessage("Could not find log folder " + logFolder.getName(), true, true);
                    }
                });
    }

    @Override
    public void enable() {
        long[] daysToDelete = new long[]{
                AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".AAC"),
                AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".AACAdditionPro"),
                AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".Server")
        };

        File[] logFolderLocations = new File[]{
                new File("plugins/AAC", "logs"),
                new File("plugins/AACAdditionPro", "logs"),
                new File("logs")
        };

        for (byte b = 0; b < daysToDelete.length; b++) {
            if (daysToDelete[b] > 0) {
                logDeletionTimes.put(logFolderLocations[b], TimeUnit.DAYS.toMillis(daysToDelete[b]));
            }
        }

        task_number = Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), this, 1, TimeUnit.DAYS.toMillis(1));
    }

    @Override
    public void disable() {
        Bukkit.getScheduler().cancelTask(task_number);
    }

    @Override
    public String getConfigString() {
        return "LogBot";
    }
}
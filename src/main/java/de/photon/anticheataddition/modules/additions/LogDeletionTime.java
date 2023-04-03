package de.photon.anticheataddition.modules.additions;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.util.messaging.Log;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class LogDeletionTime {

    private final File logFolder;
    private final long timeToDelete;

    public LogDeletionTime(File logFolder, long timeToDelete) {
        this.logFolder = logFolder;
        this.timeToDelete = timeToDelete;
    }

    public LogDeletionTime(String filePath, String configPath) {
        this(new File(filePath), TimeUnit.DAYS.toMillis(AntiCheatAddition.getInstance().getConfig().getLong("LogBot" + configPath, 10)));
    }

    public boolean isActive() {
        return timeToDelete > 0;
    }

    public void handleLog(final long currentTime) {
        // The folder exists.
        if (!logFolder.exists()) {
            Log.severe(() -> "Could not find log folder " + logFolder.getName());
            return;
        }

        final File[] files = logFolder.listFiles();
        if (files == null) return;

        for (File file : files) {
            final String fileName = file.getName();
            // Be sure it is a log file of AntiCheatAddition (.log) or a log file of the server (.log.gz)
            if ((fileName.endsWith(".log") || fileName.endsWith(".log.gz")) && currentTime - file.lastModified() > timeToDelete) {
                if (file.delete()) Log.info(() -> "Deleted " + fileName);
                else Log.severe(() -> "Could not delete old file " + fileName);
            }
        }
    }
}

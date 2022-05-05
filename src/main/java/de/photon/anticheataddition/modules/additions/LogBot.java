package de.photon.anticheataddition.modules.additions;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.util.mathematics.TimeUtil;
import lombok.EqualsAndHashCode;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class LogBot extends Module
{
    public static final LogBot INSTANCE = new LogBot();

    private final Set<LogDeletionTime> logDeletionTimes = Stream.of(new LogDeletionTime("plugins/AntiCheatAddition/logs", ".AntiCheatAddition"),
                                                                    new LogDeletionTime("logs", ".Server"))
                                                                // Actually active.
                                                                .filter(LogDeletionTime::isActive)
                                                                .collect(Collectors.toUnmodifiableSet());

    private int taskNumber;

    private LogBot()
    {
        super("LogBot");
    }

    @Override
    public void enable()
    {
        // Start a daily executed task to clean up the logs.
        taskNumber = Bukkit.getScheduler().scheduleSyncRepeatingTask(AntiCheatAddition.getInstance(), () -> {
            final long currentTime = System.currentTimeMillis();
            for (LogDeletionTime logDeletionTime : logDeletionTimes) logDeletionTime.handleLog(currentTime);
        }, 1, TimeUtil.toTicks(TimeUnit.DAYS, 1));
    }

    @Override
    public void disable()
    {
        Bukkit.getScheduler().cancelTask(taskNumber);
    }

    @EqualsAndHashCode
    private static class LogDeletionTime
    {
        private final File logFolder;
        private final long timeToDelete;

        private LogDeletionTime(String filePath, String configPath)
        {
            this.logFolder = new File(filePath);
            this.timeToDelete = TimeUnit.DAYS.toMillis(AntiCheatAddition.getInstance().getConfig().getLong("LogBot" + configPath, 10));
        }

        public boolean isActive()
        {
            return timeToDelete > 0;
        }

        public void handleLog(final long currentTime)
        {
            // The folder exists.
            if (!logFolder.exists()) {
                AntiCheatAddition.getInstance().getLogger().severe("Could not find log folder " + logFolder.getName());
                return;
            }

            final File[] files = logFolder.listFiles();
            if (files == null) return;

            for (File file : files) {
                final String fileName = file.getName();
                // Be sure it is a log file of AntiCheatAddition (.log) or a log file of the server (.log.gz)
                if ((fileName.endsWith(".log") || fileName.endsWith(".log.gz")) && currentTime - file.lastModified() > timeToDelete) {
                    final boolean result = file.delete();

                    if (result) AntiCheatAddition.getInstance().getLogger().info("Deleted " + fileName);
                    else AntiCheatAddition.getInstance().getLogger().severe("Could not delete old file " + fileName);
                }
            }
        }
    }
}
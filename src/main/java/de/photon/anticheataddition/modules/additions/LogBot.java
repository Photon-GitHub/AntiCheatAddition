package de.photon.anticheataddition.modules.additions;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.util.mathematics.TimeUtil;
import de.photon.anticheataddition.util.messaging.Log;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public final class LogBot extends Module {
    public static final LogBot INSTANCE = new LogBot();

    private static final List<LogDeletionTime> LOG_DELETION_TIMES = Stream
            .of(new LogDeletionTime("plugins/AntiCheatAddition/logs", ".AntiCheatAddition"),
                    new LogDeletionTime("logs", ".Server"))
            .filter(LogDeletionTime::isActive)
            .toList();

    private int taskNumber;

    private LogBot() {
        super("LogBot");
    }

    @Override
    public void enable() {
        // Start a daily executed task to clean up the logs.
        taskNumber = Bukkit.getScheduler().scheduleSyncRepeatingTask(AntiCheatAddition.getInstance(), this::handleLogs, 1, TimeUtil.toTicks(1, TimeUnit.DAYS));
    }

    @Override
    public void disable() {
        Bukkit.getScheduler().cancelTask(taskNumber);
    }

    private void handleLogs() {
        final long currentTime = System.currentTimeMillis();
        for (LogDeletionTime logDeletionTime : LOG_DELETION_TIMES) {
            logDeletionTime.handleLog(currentTime);
        }
    }
}

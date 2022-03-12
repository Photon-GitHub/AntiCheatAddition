package de.photon.anticheataddition.modules.additions;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.modules.Module;
import de.photon.anticheataddition.util.messaging.DebugSender;
import lombok.val;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class LogBot extends Module
{
    private final Map<File, Long> logDeletionTimes = Map.of(
            // Put the respective times in milliseconds into the map.
            new File("plugins/AACAdditionPro", "logs"), TimeUnit.DAYS.toMillis(AntiCheatAddition.getInstance().getConfig().getLong(this.getConfigString() + ".AACAdditionPro")),
            new File("logs"), TimeUnit.DAYS.toMillis(AntiCheatAddition.getInstance().getConfig().getLong(this.getConfigString() + ".Server")));

    private int taskNumber;

    public LogBot()
    {
        super("LogBot");
    }

    @Override
    public void enable()
    {
        // Start a daily executed task to clean up the logs.
        taskNumber = Bukkit.getScheduler().scheduleSyncRepeatingTask(AntiCheatAddition.getInstance(), () -> {
            val currentTime = System.currentTimeMillis();

            logDeletionTimes.forEach(
                    (logFolder, timeToDelete) ->
                    {
                        // Not disabled
                        if (timeToDelete < 0) return;

                        // The folder exists.
                        if (!logFolder.exists()) {
                            DebugSender.getInstance().sendDebug("Could not find log folder " + logFolder.getName(), true, true);
                            return;
                        }

                        val files = Optional.ofNullable(logFolder.listFiles()).orElse(new File[0]);

                        for (val file : files) {
                            val fileName = file.getName();
                            // Be sure it is a log file of AACAdditionPro (.log) or a log file of the server (.log.gz)
                            if ((fileName.endsWith(".log") || fileName.endsWith(".log.gz")) && currentTime - file.lastModified() > timeToDelete) {
                                val result = file.delete();
                                DebugSender.getInstance().sendDebug((result ? "Deleted " : "Could not delete old file ") + fileName, true, !result);
                            }
                        }
                    });
        }, 1, TimeUnit.DAYS.toSeconds(1) * 20);
    }

    @Override
    public void disable()
    {
        Bukkit.getScheduler().cancelTask(taskNumber);
    }
}
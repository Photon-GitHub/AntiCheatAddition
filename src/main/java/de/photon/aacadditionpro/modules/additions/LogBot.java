package de.photon.aacadditionpro.modules.additions;

import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.util.messaging.DebugSender;
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
            new File("plugins/AACAdditionPro", "logs"), TimeUnit.DAYS.toMillis(AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".AACAdditionPro")),
            new File("logs"), TimeUnit.DAYS.toMillis(AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".Server")));

    private int taskNumber;

    public LogBot()
    {
        super("LogBot");
    }

    @Override
    public void enable()
    {
        // Start a daily executed task to clean up the logs.
        taskNumber = Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), () -> {
            val currentTime = System.currentTimeMillis();

            logDeletionTimes.forEach(
                    (logFolder, timeToDelete) ->
                    {
                        // Not disabled
                        if (timeToDelete >= 0) {
                            // The folder exists.
                            if (logFolder.exists()) {
                                val files = Optional.ofNullable(logFolder.listFiles()).orElse(new File[0]);

                                for (val file : files) {
                                    val fileName = file.getName();
                                    // Be sure it is a log file of AACAdditionPro (.log) or a log file of the server (.log.gz)
                                    if ((fileName.endsWith(".log") || fileName.endsWith(".log.gz")) &&
                                        currentTime - file.lastModified() > timeToDelete)
                                    {
                                        val result = file.delete();
                                        DebugSender.getInstance().sendDebug((result ? "Deleted " : "Could not delete old file ") + fileName, true, !result);
                                    }
                                }
                            } else DebugSender.getInstance().sendDebug("Could not find log folder " + logFolder.getName(), true, true);
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
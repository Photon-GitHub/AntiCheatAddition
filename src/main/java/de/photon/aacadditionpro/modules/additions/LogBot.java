package de.photon.aacadditionpro.modules.additions;

import com.google.common.collect.ImmutableMap;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleLoader;
import de.photon.aacadditionpro.util.messaging.DebugSender;
import lombok.val;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LogBot extends Module
{
    private final Map<File, Long> logDeletionTimes = ImmutableMap.of(
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
                        // If the logFolder exists
                        if (logFolder.exists()) {
                            val files = logFolder.listFiles();

                            if (files != null) {
                                // The folder is not empty
                                String nameOfFile;
                                for (final File file : files) {
                                    nameOfFile = file.getName();
                                    // Be sure it is a log file of AAC or AACAdditionPro (.log) or a log file of the server (.log.gz)
                                    if ((nameOfFile.endsWith(".log") || nameOfFile.endsWith(".log.gz")) &&
                                        // Minimum time
                                        currentTime - file.lastModified() > timeToDelete)
                                    {
                                        val result = file.delete();
                                        DebugSender.getInstance().sendDebug((result ? "Deleted " : "Could not delete old file ") + nameOfFile, true, !result);
                                    }
                                }
                            }
                        } else {
                            DebugSender.getInstance().sendDebug("Could not find log folder " + logFolder.getName(), true, true);
                        }
                    });
        }, 1, TimeUnit.DAYS.toSeconds(1) * 20);
    }

    @Override
    public void disable()
    {
        Bukkit.getScheduler().cancelTask(taskNumber);
    }

    @Override
    protected ModuleLoader createModuleLoader()
    {
        return ModuleLoader.builder(this).build();
    }
}
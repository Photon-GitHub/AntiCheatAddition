package de.photon.AACAdditionPro.additions;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.Module;
import de.photon.AACAdditionPro.ModuleType;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LogBot implements Module, Runnable
{
    // HashMap's real capacity is always a power of 2
    private final Map<File, Long> logDeletionTimes = new HashMap<>(4, 1F);
    private int task_number;

    @Override
    public void run()
    {
        final long currentTime = System.currentTimeMillis();

        logDeletionTimes.forEach(
                (logFolder, timeToDelete) ->
                {
                    // If the logFolder exists
                    if (logFolder.exists())
                    {
                        final File[] files = logFolder.listFiles();

                        if (files != null)
                        {
                            // The folder is not empty
                            for (final File file : files)
                            {
                                final String nameOfFile = file.getName();
                                // Be sure it is a log file of AAC or AACAdditionPro (.log) or a log file of the server (.log.gz)
                                if ((nameOfFile.endsWith(".log") || nameOfFile.endsWith(".log.gz")) &&
                                    // Minimum time
                                    currentTime - file.lastModified() > timeToDelete)
                                {
                                    if (file.delete())
                                    {
                                        VerboseSender.sendVerboseMessage("Deleted " + nameOfFile);
                                    }
                                    else
                                    {
                                        VerboseSender.sendVerboseMessage("Could not delete old file " + nameOfFile, true, true);
                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        VerboseSender.sendVerboseMessage("Could not find log folder " + logFolder.getName(), true, true);
                    }
                });
    }

    @Override
    public void enable()
    {
        // Put the respective times in milliseconds into the map.
        logDeletionTimes.put(new File("plugins/AAC", "logs"), TimeUnit.DAYS.toMillis(AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".AAC")));
        logDeletionTimes.put(new File("plugins/AACAdditionPro", "logs"), TimeUnit.DAYS.toMillis(AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".AACAdditionPro")));
        logDeletionTimes.put(new File("logs"), TimeUnit.DAYS.toMillis(AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".Server")));

        // Start a daily executed task to clean up the logs.
        task_number = Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), this, 1, TimeUnit.DAYS.toMillis(1));
    }

    @Override
    public void disable()
    {
        Bukkit.getScheduler().cancelTask(task_number);
    }

    @Override
    public ModuleType getModuleType()
    {
        return ModuleType.LOG_BOT;
    }
}
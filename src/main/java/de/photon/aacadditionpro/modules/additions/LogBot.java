package de.photon.aacadditionpro.modules.additions;

import com.google.common.collect.ImmutableMap;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.modules.Module;
import de.photon.aacadditionpro.modules.ModuleType;
import de.photon.aacadditionpro.util.VerboseSender;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LogBot implements Module, Runnable
{
    // HashMap's real capacity is always a power of 2
    private final Map<File, Long> logDeletionTimes = ImmutableMap.of(
            // Put the respective times in milliseconds into the map.
            new File("plugins/AAC", "logs"), TimeUnit.DAYS.toMillis(AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".AAC")),
            new File("plugins/AACAdditionPro", "logs"), TimeUnit.DAYS.toMillis(AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".AACAdditionPro")),
            new File("logs"), TimeUnit.DAYS.toMillis(AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".Server")));

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
                                    final boolean result = file.delete();
                                    VerboseSender.getInstance().sendVerboseMessage((result ?
                                                                                    "Deleted " :
                                                                                    "Could not delete old file ") + nameOfFile,
                                                                                   true,
                                                                                   !result);
                                }
                            }
                        }
                    }
                    else
                    {
                        VerboseSender.getInstance().sendVerboseMessage("Could not find log folder " + logFolder.getName(), true, true);
                    }
                });
    }

    @Override
    public void enable()
    {
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
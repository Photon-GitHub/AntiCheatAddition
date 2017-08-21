package de.photon.AACAdditionPro.addition.additions;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.addition.Addition;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LogBot implements Addition, Runnable
{
    private final Map<File, Long> logDeletionTimes = new HashMap<>();
    private int task_number;

    @Override
    public void run()
    {
        for (Map.Entry<File, Long> entry : logDeletionTimes.entrySet()) {
            final long currentTime = System.currentTimeMillis();

            // If the logFolder exists
            if (entry.getKey().exists()) {
                final File[] files = entry.getKey().listFiles();

                if (files != null) {
                    // The folder is not empty
                    for (final File file : files) {
                        // Be sure it is a log file of AAC or AACAdditionPro
                        if ((file.getName().endsWith(".log") ||
                             // Log files of the server
                             file.getName().endsWith(".log.gz")
                            ) &&
                            // Minimum time
                            currentTime - file.lastModified() > entry.getValue())
                        {
                            if (file.delete()) {
                                VerboseSender.sendVerboseMessage("Deleted " + file.getName());
                            } else {
                                VerboseSender.sendVerboseMessage("Could not delete old file " + file.getName(), true, true);
                            }
                        }
                    }
                }
            } else {
                VerboseSender.sendVerboseMessage("Could not find folder " + entry.getKey().getName(), true, true);
            }
        }
    }

    @Override
    public void enable()
    {
        // AAC-Folder for logs.
        logDeletionTimes.put(new File("plugins/AAC", "logs"), TimeUnit.DAYS.toMillis(AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".AAC")));
        // AACAdditionPro folder for logs.
        logDeletionTimes.put(new File("plugins/AACAdditionPro", "logs"), TimeUnit.DAYS.toMillis(AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".AACAdditionPro")));
        // Server log folder
        logDeletionTimes.put(new File("logs"), TimeUnit.DAYS.toMillis(AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".Server")));

        // Remove the entries if the time is negative (disable a certain feature)
        logDeletionTimes.forEach((key, value) -> {
            if (value < 0) {
                logDeletionTimes.remove(key);
            }
        });


        task_number = Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), this, 1, 864000);
    }

    @Override
    public void disable()
    {
        Bukkit.getScheduler().cancelTask(task_number);
    }

    @Override
    public String getConfigString()
    {
        return "LogBot";
    }
}
package de.photon.AACAdditionPro.addition.additions;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.addition.Addition;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.Bukkit;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class LogBot implements Addition, Runnable
{
    private static final File[] AFFECTED_FOLDERS = new File[]
            {
                    // AAC-Folder for logs.
                    new File("plugins/AAC", "logs"),

                    // AACAdditionPro folder for logs.
                    new File("plugins/AACAdditionPro", "logs")
            };

    private final long millis_until_delete = TimeUnit.DAYS.toMillis(AACAdditionPro.getInstance().getConfig().getLong(this.getConfigString() + ".days_until_delete"));

    private int task_number;

    @Override
    public void run()
    {
        for (File logFolder : AFFECTED_FOLDERS) {
            final long current_time = System.currentTimeMillis();

            if (logFolder.exists()) {
                final File[] files = logFolder.listFiles();

                if (files != null) {
                    // The folder is not empty
                    for (final File file : files) {
                        // Be sure it is a log file
                        if (file.getName().endsWith(".log") &&
                            // Minimum time
                            current_time - file.lastModified() > this.millis_until_delete)
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
                AACAdditionPro.getInstance().getLogger().severe("Could not find folder.");
            }
        }
    }

    @Override
    public void enable()
    {
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
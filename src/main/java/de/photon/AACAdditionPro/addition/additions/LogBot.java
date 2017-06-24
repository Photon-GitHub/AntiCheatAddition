package de.photon.AACAdditionPro.addition.additions;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.addition.Addition;
import de.photon.AACAdditionPro.util.verbose.VerboseSender;
import org.bukkit.Bukkit;

import java.io.File;

public class LogBot implements Addition, Runnable
{
    private static final File AAC_LOG_FOLDER = new File("plugins/AAC", "logs");
    private static final File AACADDITIONPRO_LOG_FOLDER = new File("plugins/AACAdditionPro", "logs");
    private final int millis_until_delete = (AACAdditionPro.getInstance().getConfig().getInt(this.getConfigString() + ".days_until_delete") << 10) * 27 * 3125;

    private int task_number;

    @Override
    public void run()
    {
        // AACAdditionPro
        deleteOldFiles(AACADDITIONPRO_LOG_FOLDER, millis_until_delete);

        // AAC
        deleteOldFiles(AAC_LOG_FOLDER, millis_until_delete);
    }

    private static void deleteOldFiles(final File folder, final long millis_until_delete)
    {
        final long current_time = System.currentTimeMillis();

        if (folder.exists()) {
            final File[] files = folder.listFiles();

            if (files != null) {
                // The folder is not empty
                for (final File file : files) {
                    if (file.getName().endsWith(".log") &&
                        current_time - file.lastModified() > millis_until_delete)
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

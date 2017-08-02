package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import org.bukkit.Bukkit;

public class FlyPatchData
{
    private int threshold = AACAdditionPro.getInstance().getConfig().getInt("FlyPatch.toggles");

    private volatile int secondCounter = 0;

    /**
     * Displays the signum of the last movement (false = negative or zero)
     */
    private boolean lastSignum = false;

    public FlyPatchData()
    {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(AACAdditionPro.getInstance(), () -> secondCounter = 0, 0, 20);
    }

    /**
     * Increases the counter if a change was detected and inverts lastSignum for a new run.
     */
    public boolean countNewChange(double signum)
    {
        if (signum > 0 && lastSignum || signum < 0 && !lastSignum) {
            lastSignum = !lastSignum;
            if (++secondCounter > threshold) {
                return true;
            }
        }
        return false;
    }
}

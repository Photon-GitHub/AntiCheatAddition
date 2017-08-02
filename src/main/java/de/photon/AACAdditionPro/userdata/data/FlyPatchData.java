package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.AACAdditionPro;

public class FlyPatchData
{
    private int threshold = AACAdditionPro.getInstance().getConfig().getInt("FlyPatch.toggles");

    private volatile int secondCounter = 0;

    /**
     * Displays the signum of the last
     */
    private boolean lastSignum = false;

    public FlyPatchData()
    {
        recursiveStartThread();
    }

    private void recursiveStartThread()
    {
        new Thread(
                () ->
                {
                    secondCounter = 0;
                    try {
                        wait(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
    }

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

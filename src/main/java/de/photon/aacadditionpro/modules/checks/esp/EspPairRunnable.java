package de.photon.aacadditionpro.modules.checks.esp;

import de.photon.aacadditionpro.util.visibility.HideMode;
import org.bukkit.entity.Player;

class EspPairRunnable implements Runnable
{
    private final Player observer;
    private final Player watched;

    private final int playerTrackingRange;

    public EspPairRunnable(Player observer, Player watched)
    {
        this.observer = observer;
        this.watched = watched;
        this.playerTrackingRange = Esp.getInstance().playerTrackingRanges.getOrDefault(observer.getWorld(), Esp.getInstance().defaultTrackingRange);
    }

    @Override
    public void run()
    {
        // The users are always in the same world (see above)
        final double pairDistanceSquared = observer.getLocation().distanceSquared(watched.getPlayer().getLocation());

        // Less than 1 block distance
        // Everything (smaller than 1)^2 will result in something smaller than 1
        if (pairDistanceSquared < 1) {
            Esp.getInstance().updatePairHideMode(observer, watched, HideMode.NONE);
            Esp.getInstance().cycleSemaphore.release();
            return;
        }

        if (pairDistanceSquared > this.playerTrackingRange) {
            Esp.getInstance().updatePairHideMode(observer, watched, Esp.getInstance().hideAfterRenderDistance ?
                                                                    HideMode.FULL :
                                                                    HideMode.NONE);
            Esp.getInstance().cycleSemaphore.release();
            return;
        }

        // Update hide mode in both directions.
        Esp.getInstance().updateHideMode(observer, watched.getPlayer(),
                                         CanSee.canSee(observer, watched) ?
                                         // Is the user visible
                                         HideMode.NONE :
                                         // If the observed player is sneaking hide him fully
                                         (watched.getPlayer().isSneaking() ?
                                          HideMode.FULL :
                                          HideMode.INFORMATION_ONLY));

        Esp.getInstance().updateHideMode(watched, observer.getPlayer(),
                                         CanSee.canSee(watched, observer) ?
                                         // Is the user visible
                                         HideMode.NONE :
                                         // If the observed player is sneaking hide him fully
                                         (observer.getPlayer().isSneaking() ?
                                          HideMode.FULL :
                                          HideMode.INFORMATION_ONLY));

        Esp.getInstance().cycleSemaphore.release();
    }
}

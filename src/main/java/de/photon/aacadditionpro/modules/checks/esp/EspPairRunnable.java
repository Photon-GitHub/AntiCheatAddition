package de.photon.aacadditionpro.modules.checks.esp;

import de.photon.aacadditionpro.util.visibility.HideMode;
import lombok.RequiredArgsConstructor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
class EspPairRunnable implements Runnable
{
    private final Player observer;
    private final Player watched;

    @Override
    public void run()
    {
        final int playerTrackingRange = Esp.getInstance().playerTrackingRanges.getOrDefault(observer.getWorld(), Esp.getInstance().defaultTrackingRange);

        // The users are always in the same world (see above)
        final double pairDistanceSquared = observer.getLocation().distanceSquared(watched.getLocation());

        final HideMode observerToWatched;
        final HideMode watchedToObserver;

        // Less than 1 block distance
        // Everything (smaller than 1)^2 will result in something smaller than 1
        if (pairDistanceSquared < 1) {
            observerToWatched = HideMode.NONE;
            watchedToObserver = HideMode.NONE;
        } else if (pairDistanceSquared >= playerTrackingRange) {
            observerToWatched = Esp.getInstance().hideAfterRenderDistance ? HideMode.FULL : HideMode.NONE;
            watchedToObserver = observerToWatched;
        } else {
            observerToWatched = getHideModeCanSee(observer, watched);
            watchedToObserver = getHideModeCanSee(watched, observer);
        }

        // Update hide mode in both directions.
        Esp.getInstance().updateHideMode(observer, watched.getPlayer(), observerToWatched);
        Esp.getInstance().updateHideMode(watched, observer.getPlayer(), watchedToObserver);

        Esp.getInstance().cycleSemaphore.release();
    }

    @NotNull
    private HideMode getHideModeCanSee(Player observingPlayer, Player watchedPlayer)
    {
        // Is the user visible
        if (CanSee.canSee(observingPlayer, watchedPlayer)) {
            return HideMode.NONE;
        }

        // If the observed player is sneaking hide him fully
        return observer.isSneaking() ? HideMode.FULL : HideMode.INFORMATION_ONLY;
    }
}

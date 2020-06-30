package de.photon.aacadditionpro.modules.checks.esp;

import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.util.visibility.HideMode;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
class EspPairRunnable implements Runnable
{

    private final User observingUser;
    private final User watched;

    private final int playerTrackingRange = Esp.getInstance().playerTrackingRanges.getOrDefault(observingUser.getPlayer().getWorld().getUID(), Esp.getInstance().defaultTrackingRange);

    @Override
    public void run()
    {
        // The users are always in the same world (see above)
        final double pairDistanceSquared = observingUser.getPlayer().getLocation().distanceSquared(watched.getPlayer().getLocation());

        // Less than 1 block distance
        // Everything (smaller than 1)^2 will result in something smaller than 1
        if (pairDistanceSquared < 1) {
            Esp.getInstance().updatePairHideMode(observingUser, watched, HideMode.NONE);
            return;
        }

        if (pairDistanceSquared > this.playerTrackingRange) {
            Esp.getInstance().updatePairHideMode(observingUser, watched, Esp.getInstance().hideAfterRenderDistance ?
                                                                         HideMode.FULL :
                                                                         HideMode.NONE);
            return;
        }

        // Update hide mode in both directions.
        Esp.getInstance().updateHideMode(observingUser, watched.getPlayer(),
                                         CanSee.canSee(observingUser, watched) ?
                                         // Is the user visible
                                         HideMode.NONE :
                                         // If the observed player is sneaking hide him fully
                                         (watched.getPlayer().isSneaking() ?
                                          HideMode.FULL :
                                          HideMode.INFORMATION_ONLY));

        Esp.getInstance().updateHideMode(watched, observingUser.getPlayer(),
                                         CanSee.canSee(watched, observingUser) ?
                                         // Is the user visible
                                         HideMode.NONE :
                                         // If the observed player is sneaking hide him fully
                                         (observingUser.getPlayer().isSneaking() ?
                                          HideMode.FULL :
                                          HideMode.INFORMATION_ONLY));
    }
}

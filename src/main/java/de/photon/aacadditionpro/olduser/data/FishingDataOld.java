package de.photon.aacadditionpro.olduser.data;

import de.photon.aacadditionpro.olduser.TimeDataOld;
import de.photon.aacadditionpro.olduser.UserOld;

public class FishingDataOld extends TimeDataOld
{

    /**
     * This represents the amount of fails between two successful fishing times.
     * If this is too high an explanation attempt of an afk fish farm is more sensible than a bot.
     */
    public int failedCounter = 0;


    public FishingDataOld(final UserOld user)
    {
        // [0] = Timestamp of last fish bite (PlayerFishEvent.State.BITE)
        // [1] = Timestamp used for the time between a caught fish (pull in fishing rod) and a new fishing attempt
        super(user, 0,0);
    }
}

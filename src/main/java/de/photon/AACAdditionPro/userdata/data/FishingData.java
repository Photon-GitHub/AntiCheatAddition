package de.photon.AACAdditionPro.userdata.data;

import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.AdditionHackType;
import de.photon.AACAdditionPro.userdata.User;
import de.photon.AACAdditionPro.util.storage.management.DoubleBuffer;

public class FishingData extends TimeData
{
    public final DoubleBuffer consistencyBuffer = new DoubleBuffer(AACAdditionPro.getInstance().getConfig().getInt(AdditionHackType.AUTO_FISH.getConfigString() + ".parts.consistency.consistency_events"))
    {
        @Override
        public boolean verifyObject(Double object)
        {
            return true;
        }
    };

    /**
     * Adds a new delta to the consistencyBuffer
     *
     * @return true if the amount of elements needed for a check is reached.
     */
    public boolean bufferConsistencyData()
    {
        return this.consistencyBuffer.bufferObject((double) (System.currentTimeMillis() - this.getTimeStamp(1)));
    }

    public FishingData(User theUser)
    {
        super(false, theUser, 0, 0);
    }
}

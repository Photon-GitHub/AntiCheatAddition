package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.datawrappers.RotationChange;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.mathematics.RotationUtil;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;

public class LookPacketData extends TimeData
{
    private static final byte QUEUE_CAPACITY = 20;

    // PacketAnalysis
    @Getter
    private float realLastYaw;
    @Getter
    private float realLastPitch;

    @Getter
    private final Deque<RotationChange> rotationChangeQueue = new LinkedList<>();

    // First index is for timeout, second one for significant rotation changes (scaffold)
    public LookPacketData(final User user)
    {
        super(user, 0);

        // Prevent initial problems.
        this.rotationChangeQueue.addLast(new RotationChange(0, 0));
    }

    public void updateRotations(final float yaw, final float pitch)
    {
        this.realLastYaw = yaw;
        this.realLastPitch = pitch;

        this.bufferRotationChange(new RotationChange(yaw, pitch));

        // Huge angle change
        if (RotationUtil.getDirection(this.getLastYaw(), this.getLastPitch()).angle(RotationUtil.getDirection(yaw, pitch)) > 35)
        {
            this.updateTimeStamp(0);
        }
    }

    public float getLastYaw()
    {
        return this.rotationChangeQueue.getLast().getYaw();
    }

    public float getLastPitch()
    {
        return this.rotationChangeQueue.getLast().getPitch();
    }

    /**
     * Adds or merges a new {@link RotationChange}
     */
    public synchronized void bufferRotationChange(final RotationChange rotationChange)
    {
        // Same tick -> merge
        if (rotationChange.getTime() - this.rotationChangeQueue.getLast().getTime() < 55)
        {
            this.rotationChangeQueue.getLast().merge(rotationChange);
        }
        else
        {
            this.rotationChangeQueue.addLast(rotationChange);
        }

        while (rotationChangeQueue.size() > QUEUE_CAPACITY)
        {
            rotationChangeQueue.removeFirst();
        }
    }

    /**
     * Calculates the total rotation change in the last time.
     *
     * @return an array which contains information about the angle changes. <br>
     * [0] is the sum of the angle changes <br>
     * [1] is the sum of the angle offsets
     */
    public synchronized float[] getAngleInformation()
    {
        final float[] result = new float[2];

        // Ticks that must be added to fill up the gaps in the queue.
        short gapFillers = 0;

        final Collection<Float> rotationCache = new ArrayList<>(this.rotationChangeQueue.size());
        final RotationChange[] elementArray = this.rotationChangeQueue.toArray(new RotationChange[0]);

        // Start at 1 as of the 0 element being the first "last element".
        for (int i = 1; i < elementArray.length; i++)
        {
            if (MathUtils.offset(System.currentTimeMillis(), elementArray[i].getTime()) > 1000)
            {
                continue;
            }

            short ticks = (short) (MathUtils.offset(elementArray[i].getTime(),
                                                    // Using -1 for the last element is fine as there is always the last element.
                                                    elementArray[i - 1].getTime()) / 50);

            // The current tick should be ignored, no gap filler.
            if (ticks > 1)
            {
                // How many ticks have been left out?
                gapFillers += (ticks - 1);
            }

            // Angle calculations
            float angle = elementArray[i - 1].angle(elementArray[i]);
            rotationCache.add(angle);
            // Angle change sum
            result[0] += angle;
        }

        // Angle offset sum
        result[1] = (float) MathUtils.offset(
                // The average of the elements
                result[0] / (rotationCache.size() + gapFillers) * rotationCache.size(),
                // The sum of all elements
                result[0]);

        return result;
    }
}

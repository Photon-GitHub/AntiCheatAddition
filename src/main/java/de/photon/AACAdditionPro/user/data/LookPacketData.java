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

    // EqualRotation
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
    public void bufferRotationChange(final RotationChange rotationChange)
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
    public float[] getAngleInformation()
    {
        final float[] result = new float[2];

        final Collection<Float> rotationCache = new ArrayList<>(this.rotationChangeQueue.size());
        // Ticks that must be added to fill up the gaps in the queue.
        short gapFillers = 0;

        final RotationChange[] elementArray = this.rotationChangeQueue.toArray(new RotationChange[this.rotationChangeQueue.size()]);

        // Start at 1 as of the 0 element being the first "last element".
        for (int i = 1; i < elementArray.length; i++)
        {
            if (MathUtils.offset(rotationChangeQueue.getLast().getTime(), elementArray[i].getTime()) > 1000)
            {
                continue;
            }

            // How many ticks have been left out?
            // Using -1 for the last element is fine as there is always the last element.
            gapFillers += MathUtils.offset(elementArray[i].getTime(), elementArray[i - 1].getTime()) / 50;

            rotationCache.add(elementArray[i - 1].angle(elementArray[i]));
        }

        // Angle change sum
        for (Float rotation : rotationCache)
        {
            result[0] += rotation;
        }

        float average = result[0] / (rotationCache.size() + gapFillers);

        // Angle offset sum
        for (Float rotation : rotationCache)
        {
            result[1] += MathUtils.offset(average, rotation);
        }

        return result;
    }
}

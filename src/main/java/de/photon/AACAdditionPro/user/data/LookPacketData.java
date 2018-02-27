package de.photon.AACAdditionPro.user.data;

import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.util.datawrappers.RotationChange;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.mathematics.RotationUtil;
import lombok.Getter;

import java.util.Deque;
import java.util.DoubleSummaryStatistics;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LookPacketData extends TimeData
{
    private static final int QUEUE_CAPACITY = 20;

    // EqualRotation
    @Getter
    private float realLastYaw;
    @Getter
    private float realLastPitch;

    @Getter
    private final Deque<RotationChange> rotationChangeQueue = new ConcurrentLinkedDeque<>();

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
        if (rotationChange.getTime() - this.rotationChangeQueue.getLast().getTime() < 50)
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
     */
    public DoubleSummaryStatistics getAngleChange()
    {
        final DoubleSummaryStatistics result = new DoubleSummaryStatistics();
        final Iterator<RotationChange> rotationChangeQueueIterator = this.rotationChangeQueue.iterator();

        RotationChange lastRotationChange = rotationChangeQueueIterator.next();
        RotationChange currentRotationChange;
        while (rotationChangeQueueIterator.hasNext())
        {
            currentRotationChange = rotationChangeQueueIterator.next();
            if (MathUtils.offset(rotationChangeQueue.getLast().getTime(), currentRotationChange.getTime()) > 1000)
            {
                continue;
            }

            // How many ticks have been left out?
            for (int i = 0; i < (MathUtils.offset(currentRotationChange.getTime(), lastRotationChange.getTime()) / 50); i++)
            {
                // Fill in the gaps.
                result.accept(0);
            }

            result.accept(RotationUtil.getDirection(lastRotationChange.getYaw(), lastRotationChange.getPitch()).angle(RotationUtil.getDirection(currentRotationChange.getYaw(), currentRotationChange.getPitch())));
            lastRotationChange = currentRotationChange;
        }
        return result;
    }

    public DoubleSummaryStatistics getOffsetAngleChange(final double angleChangeAverage)
    {
        final DoubleSummaryStatistics result = new DoubleSummaryStatistics();
        final Iterator<RotationChange> rotationChangeQueueIterator = this.rotationChangeQueue.iterator();

        RotationChange lastRotationChange = rotationChangeQueueIterator.next();
        RotationChange currentRotationChange;
        while (rotationChangeQueueIterator.hasNext())
        {
            currentRotationChange = rotationChangeQueueIterator.next();
            if (MathUtils.offset(rotationChangeQueue.getLast().getTime(), currentRotationChange.getTime()) > 1000)
            {
                continue;
            }

            result.accept(MathUtils.offset(angleChangeAverage, RotationUtil.getDirection(lastRotationChange.getYaw(), lastRotationChange.getPitch()).angle(RotationUtil.getDirection(currentRotationChange.getYaw(), currentRotationChange.getPitch()))));
            lastRotationChange = currentRotationChange;
        }
        return result;
    }
}

package de.photon.AACAdditionPro.user.data;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.AACAdditionPro.AACAdditionPro;
import de.photon.AACAdditionPro.user.TimeData;
import de.photon.AACAdditionPro.user.User;
import de.photon.AACAdditionPro.user.UserManager;
import de.photon.AACAdditionPro.util.mathematics.MathUtils;
import de.photon.AACAdditionPro.util.mathematics.RotationUtil;
import de.photon.AACAdditionPro.util.packetwrappers.IWrapperPlayClientLook;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;

public class LookPacketData extends TimeData
{
    private static final byte QUEUE_CAPACITY = 20;

    public int smoothAimCounter = 0;

    static
    {
        LookPacketData.LookPacketDataUpdater dataUpdater = new LookPacketData.LookPacketDataUpdater();
        ProtocolLibrary.getProtocolManager().addPacketListener(dataUpdater);
    }

    // PacketAnalysisData
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

    @AllArgsConstructor
    public static class RotationChange
    {
        @Getter
        private float yaw;
        @Getter
        private float pitch;
        @Getter
        private final long time = System.currentTimeMillis();

        /**
         * Merges a {@link RotationChange} with this {@link RotationChange}.
         */
        public void merge(final RotationChange rotationChange)
        {
            this.yaw += rotationChange.yaw;
            this.pitch += rotationChange.pitch;
        }

        /**
         * Calculates the total angle between two {@link RotationChange} - directions.
         */
        public float angle(final RotationChange rotationChange)
        {
            return RotationUtil.getDirection(this.getYaw(), this.getPitch()).angle(RotationUtil.getDirection(rotationChange.getYaw(), rotationChange.getPitch()));
        }
    }

    /**
     * A singleton class to reduce the reqired {@link com.comphenix.protocol.events.PacketListener}s to a minimum.
     */
    private static class LookPacketDataUpdater extends PacketAdapter
    {
        // Beacon handling
        public LookPacketDataUpdater()
        {
            super(AACAdditionPro.getInstance(), ListenerPriority.MONITOR, PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK);
        }

        @Override
        public void onPacketReceiving(PacketEvent event)
        {
            // Correct packets
            if ((event.getPacketType() == PacketType.Play.Client.LOOK ||
                 event.getPacketType() == PacketType.Play.Client.POSITION_LOOK) &&
                // Not cancelled
                !event.isCancelled())
            {
                final User user = UserManager.getUser(event.getPlayer().getUniqueId());
                final IWrapperPlayClientLook lookWrapper = event::getPacket;

                final LookPacketData lookPacketData = user.getLookPacketData();

                final RotationChange rotationChange = new RotationChange(lookWrapper.getYaw(), lookWrapper.getPitch());

                // Same tick -> merge
                if (rotationChange.getTime() - lookPacketData.rotationChangeQueue.getLast().getTime() < 55)
                {
                    lookPacketData.rotationChangeQueue.getLast().merge(rotationChange);
                }
                else
                {
                    lookPacketData.rotationChangeQueue.addLast(rotationChange);
                }
                while (lookPacketData.rotationChangeQueue.size() > QUEUE_CAPACITY)
                {
                    lookPacketData.rotationChangeQueue.removeFirst();
                }

                // Huge angle change
                // Use the queue values here to because the other ones are already updated.
                if (RotationUtil.getDirection(lookPacketData.realLastYaw, lookPacketData.realLastPitch).angle(RotationUtil.getDirection(lookWrapper.getYaw(), lookWrapper.getPitch())) > 35)
                {
                    lookPacketData.updateTimeStamp(0);
                }

                // Update the values here so the RotationUtil calculation is functional.
                lookPacketData.realLastYaw = lookWrapper.getYaw();
                lookPacketData.realLastPitch = lookWrapper.getPitch();
            }
        }
    }
}

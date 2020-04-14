package de.photon.aacadditionpro.user.subdata;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.user.DataKey;
import de.photon.aacadditionpro.user.TimestampKey;
import de.photon.aacadditionpro.user.User;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.util.mathematics.MathUtils;
import de.photon.aacadditionpro.util.mathematics.RotationUtil;
import de.photon.aacadditionpro.util.packetwrappers.client.IWrapperPlayClientLook;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;

public class LookPacketData extends SubData
{
    private static final byte QUEUE_CAPACITY = 20;

    static {
        ProtocolLibrary.getProtocolManager().addPacketListener(new LookPacketData.LookPacketDataUpdater());
    }

    @Getter
    private final Deque<LookPacketData.RotationChange> rotationChangeQueue = new LinkedList<>();

    public LookPacketData(User user)
    {
        super(user);

        // Prevent initial problems.
        this.rotationChangeQueue.addLast(new LookPacketData.RotationChange(0, 0));
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

        // Ticks that must be added to fill up the gaps in the queue.
        short gapFillers = 0;

        synchronized (this.rotationChangeQueue) {
            final Collection<Float> rotationCache = new ArrayList<>(this.rotationChangeQueue.size());
            final LookPacketData.RotationChange[] elementArray = this.rotationChangeQueue.toArray(new LookPacketData.RotationChange[0]);


            // Start at 1 as of the 0 element being the first "last element".
            for (int i = 1; i < elementArray.length; i++) {
                if (MathUtils.offset(System.currentTimeMillis(), elementArray[i].getTime()) > 1000) {
                    continue;
                }

                short ticks = (short) (MathUtils.offset(elementArray[i].getTime(),
                                                        // Using -1 for the last element is fine as there is always the last element.
                                                        elementArray[i - 1].getTime()) / 50
                );

                // The current tick should be ignored, no gap filler.
                if (ticks > 1) {
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
        }

        return result;
    }

    @AllArgsConstructor
    public static class RotationChange
    {
        @Getter
        private final long time = System.currentTimeMillis();
        @Getter
        private float yaw;
        @Getter
        private float pitch;

        /**
         * Merges a {@link LookPacketData.RotationChange} with this {@link LookPacketData.RotationChange}.
         */
        public void merge(final LookPacketData.RotationChange rotationChange)
        {
            this.yaw += rotationChange.yaw;
            this.pitch += rotationChange.pitch;
        }

        /**
         * Calculates the total angle between two {@link LookPacketData.RotationChange} - directions.
         */
        public float angle(final LookPacketData.RotationChange rotationChange)
        {
            return RotationUtil.getDirection(this.getYaw(), this.getPitch()).angle(RotationUtil.getDirection(rotationChange.getYaw(), rotationChange.getPitch()));
        }
    }

    /**
     * A singleton class to reduce the reqired {@link com.comphenix.protocol.events.PacketListener}s to a minimum.
     */
    private static class LookPacketDataUpdater extends PacketAdapter
    {
        public LookPacketDataUpdater()
        {
            super(AACAdditionPro.getInstance(), ListenerPriority.MONITOR, PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK);
        }

        @Override
        public void onPacketReceiving(PacketEvent event)
        {
            // Not cancelled
            if (event.isCancelled() || event.isPlayerTemporary()) {
                return;
            }

            final User user = UserManager.getUser(event.getPlayer().getUniqueId());

            if (user == null) {
                return;
            }

            final IWrapperPlayClientLook lookWrapper = event::getPacket;

            final LookPacketData.RotationChange rotationChange = new LookPacketData.RotationChange(lookWrapper.getYaw(), lookWrapper.getPitch());

            // Same tick -> merge
            synchronized (user.getLookPacketData().rotationChangeQueue) {
                if (rotationChange.getTime() - user.getLookPacketData().rotationChangeQueue.getLast().getTime() < 55) {
                    user.getLookPacketData().rotationChangeQueue.getLast().merge(rotationChange);
                } else {
                    user.getLookPacketData().rotationChangeQueue.addLast(rotationChange);
                }

                while (user.getLookPacketData().rotationChangeQueue.size() > QUEUE_CAPACITY) {
                    user.getLookPacketData().rotationChangeQueue.removeFirst();
                }
            }

            // Huge angle change
            // Use the queue values here to because the other ones are already updated.
            if (RotationUtil.getDirection(user.getDataMap().getFloat(DataKey.PACKET_ANALYSIS_REAL_LAST_YAW), user.getDataMap().getFloat(DataKey.PACKET_ANALYSIS_REAL_LAST_PITCH))
                            .angle(RotationUtil.getDirection(lookWrapper.getYaw(), lookWrapper.getPitch())) > 35)
            {
                user.getTimestampMap().updateTimeStamp(TimestampKey.SCAFFOLD_SIGNIFICANT_ROTATION_CHANGE);
            }

            // Update the values here so the RotationUtil calculation is functional.
            user.getDataMap().setValue(DataKey.PACKET_ANALYSIS_REAL_LAST_YAW, lookWrapper.getYaw());
            user.getDataMap().setValue(DataKey.PACKET_ANALYSIS_REAL_LAST_PITCH, lookWrapper.getPitch());
        }
    }
}

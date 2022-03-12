package de.photon.anticheataddition.user.data.subdata;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyclient.IWrapperPlayClientLook;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.DataKey;
import de.photon.anticheataddition.user.data.TimestampKey;
import de.photon.anticheataddition.util.datastructure.buffer.RingBuffer;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.mathematics.RotationUtil;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.NonFinal;
import lombok.val;

public class LookPacketData
{
    private static final byte QUEUE_CAPACITY = 20;

    static {
        ProtocolLibrary.getProtocolManager().addPacketListener(new LookPacketDataUpdater());
    }

    @Getter
    private final RingBuffer<RotationChange> rotationChangeQueue = new RingBuffer<>(QUEUE_CAPACITY, new RotationChange(0, 0));

    /**
     * Calculates the total rotation change in the last time.
     *
     * @return an array which contains information about the angle changes. <br>
     * [0] is the sum of the angle changes <br>
     * [1] is the sum of the angle offsets
     */
    public double[] getAngleInformation()
    {
        val result = new double[2];
        final RotationChange[] changes;

        synchronized (this.rotationChangeQueue) {
            changes = this.rotationChangeQueue.toArray(new RotationChange[0]);
        }

        int rotationCount = 0;
        int gapFillers = 0;
        long ticks;
        float angle;
        val curTime = System.currentTimeMillis();
        for (int i = 1; i < changes.length; ++i) {
            // Ignore rotation changes more than 1 second ago.
            if ((curTime - changes[i].getTime()) > 1000) continue;

            // Using -1 for the last element is fine as there is always the last element.
            ticks = (changes[i - 1].getTime() - changes[i].getTime()) / 50;

            // The current tick should be ignored, no gap filler.
            // How many ticks have been left out?
            if (ticks > 1) gapFillers += (ticks - 1);

            // Angle calculations
            angle = changes[i - 1].angle(changes[i]);
            ++rotationCount;
            // Angle change sum
            result[0] += angle;
        }

        // Just immediately return the [0,0] array here to avoid dividing by 0.
        if (rotationCount == 0 && gapFillers == 0) return result;

        // Angle offset sum
        result[1] = MathUtil.absDiff(
                // The offset average times the rotations
                (result[0] / (rotationCount + gapFillers)) * rotationCount,
                // The sum of all elements
                result[0]);

        return result;
    }

    @Value
    public static class RotationChange
    {
        long time = System.currentTimeMillis();
        @NonFinal float yaw;
        @NonFinal float pitch;

        /**
         * Merges a {@link RotationChange} with this {@link RotationChange}.
         */
        public void merge(RotationChange rotationChange)
        {
            this.yaw += rotationChange.yaw;
            this.pitch += rotationChange.pitch;
        }

        /**
         * Calculates the total angle between two {@link RotationChange} - directions.
         */
        public float angle(RotationChange rotationChange)
        {
            return RotationUtil.getDirection(this.getYaw(), this.getPitch()).angle(RotationUtil.getDirection(rotationChange.getYaw(), rotationChange.getPitch()));
        }
    }

    /**
     * A singleton class to reduce the required {@link com.comphenix.protocol.events.PacketListener}s to a minimum.
     */
    private static class LookPacketDataUpdater extends PacketAdapter
    {
        public LookPacketDataUpdater()
        {
            super(AntiCheatAddition.getInstance(), ListenerPriority.MONITOR, PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK);
        }

        @Override
        public void onPacketReceiving(PacketEvent event)
        {
            val user = User.safeGetUserFromPacketEvent(event);
            if (user == null) return;

            final IWrapperPlayClientLook lookWrapper = event::getPacket;

            val rotationChange = new RotationChange(lookWrapper.getYaw(), lookWrapper.getPitch());

            // Same tick -> merge
            synchronized (user.getLookPacketData().rotationChangeQueue) {
                if (rotationChange.getTime() - user.getLookPacketData().getRotationChangeQueue().head().getTime() < 55) {
                    user.getLookPacketData().getRotationChangeQueue().head().merge(rotationChange);
                } else {
                    user.getLookPacketData().getRotationChangeQueue().add(rotationChange);
                }
            }

            // Huge angle change
            // Use the map values here to because the other ones are already updated.
            if (RotationUtil.getDirection(user.getDataMap().getFloat(DataKey.Float.LAST_PACKET_YAW), user.getDataMap().getFloat(DataKey.Float.LAST_PACKET_PITCH))
                            .angle(RotationUtil.getDirection(lookWrapper.getYaw(), lookWrapper.getPitch())) > 35)
            {
                user.getTimestampMap().at(TimestampKey.SCAFFOLD_SIGNIFICANT_ROTATION_CHANGE).update();
            }

            // Update the values here so the RotationUtil calculation is functional.
            user.getDataMap().setFloat(DataKey.Float.LAST_PACKET_YAW, lookWrapper.getYaw());
            user.getDataMap().setFloat(DataKey.Float.LAST_PACKET_PITCH, lookWrapper.getPitch());
        }
    }
}

package de.photon.anticheataddition.user.data.subdata;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.protocol.packetwrappers.sentbyclient.IWrapperPlayClientLook;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.datastructure.buffer.RingBuffer;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.mathematics.RotationUtil;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.NonFinal;

public final class LookPacketData
{
    static {
        ProtocolLibrary.getProtocolManager().addPacketListener(new LookPacketDataUpdater());
    }

    @Getter
    private final RingBuffer<RotationChange> rotationChangeQueue = new RingBuffer<>(20, new RotationChange(0, 0));

    /**
     * Calculates the total rotation change in the last time.
     *
     * @return an array which contains information about the angle changes. <br>
     * [0] is the sum of the angle changes <br>
     * [1] is the sum of the angle offsets
     */
    public double[] getAngleInformation()
    {
        final RotationChange[] changes;

        synchronized (this.rotationChangeQueue) {
            changes = this.rotationChangeQueue.toArray(new RotationChange[0]);
        }

        final long curTime = System.currentTimeMillis();

        int rotationCount = 0;
        int gapFillers = 0;
        double angleSum = 0;
        for (int i = 1; i < changes.length; ++i) {
            // Ignore rotation changes more than 1 second ago.
            if ((curTime - changes[i].getTime()) > 1000) continue;

            // Using -1 for the last element is fine as there is always the last element.
            final long ticks = changes[i - 1].timeOffset(changes[i]) / 50;

            // The current tick should be ignored, no gap filler.
            // How many ticks have been left out?
            if (ticks > 1) gapFillers += (ticks - 1);

            // This is a rotation.
            ++rotationCount;

            // Angle change sum
            angleSum += changes[i - 1].angle(changes[i]);
        }

        // Just immediately return the [0,0] array here to avoid dividing by 0.
        if (rotationCount == 0 && gapFillers == 0) return new double[]{0, 0};

        return new double[]{
                angleSum,
                // Compute the difference of angleSum and angleSum * (rotationCount / (rotationCount + gapFillers))
                MathUtil.absDiff((angleSum / (rotationCount + gapFillers)) * rotationCount, angleSum)
        };
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
            return RotationUtil.getDirection(this.yaw, this.pitch).angle(RotationUtil.getDirection(rotationChange.getYaw(), rotationChange.getPitch()));
        }

        public long timeOffset(RotationChange other)
        {
            return MathUtil.absDiff(this.time, other.time);
        }
    }

    /**
     * A singleton class to reduce the required {@link com.comphenix.protocol.events.PacketListener}s to a minimum.
     */
    private static final class LookPacketDataUpdater extends PacketAdapter
    {
        public LookPacketDataUpdater()
        {
            super(AntiCheatAddition.getInstance(), ListenerPriority.MONITOR, PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK);
        }

        @Override
        public void onPacketReceiving(PacketEvent event)
        {
            final var user = User.safeGetUserFromPacketEvent(event);
            if (user == null) return;

            final IWrapperPlayClientLook lookWrapper = event::getPacket;

            final var rotationChange = new RotationChange(lookWrapper.getYaw(), lookWrapper.getPitch());
            final var rotationQueue = user.getLookPacketData().rotationChangeQueue;

            // Same tick -> merge
            synchronized (user.getLookPacketData().rotationChangeQueue) {
                if (rotationChange.timeOffset(rotationQueue.tail()) < 55) rotationQueue.tail().merge(rotationChange);
                else rotationQueue.add(rotationChange);
            }

            // Huge angle change
            // Use the map values here to because the other ones are already updated.
            if (RotationUtil.getDirection(user.getData().floating.lastPacketYaw, user.getData().floating.lastPacketPitch)
                            .angle(RotationUtil.getDirection(lookWrapper.getYaw(), lookWrapper.getPitch())) > 35)
            {
                user.getTimeMap().at(TimeKey.SCAFFOLD_SIGNIFICANT_ROTATION_CHANGE).update();
            }

            // Update the values here so the RotationUtil calculation is functional.
            user.getData().floating.lastPacketYaw = lookWrapper.getYaw();
            user.getData().floating.lastPacketPitch = lookWrapper.getPitch();
        }
    }
}

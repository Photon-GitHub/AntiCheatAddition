package de.photon.anticheataddition.user.data.subdata;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.google.common.primitives.Doubles;
import de.photon.anticheataddition.modules.checks.scaffold.ScaffoldRotation;
import de.photon.anticheataddition.user.User;
import de.photon.anticheataddition.user.data.TimeKey;
import de.photon.anticheataddition.util.datastructure.buffer.RingBuffer;
import de.photon.anticheataddition.util.log.Log;
import de.photon.anticheataddition.util.mathematics.DataUtil;
import de.photon.anticheataddition.util.mathematics.MathUtil;
import de.photon.anticheataddition.util.mathematics.TimeUtil;
import de.photon.anticheataddition.util.protocol.PacketEventUtils;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Represents a data structure that stores and processes look packets for users.
 */
public final class LookPacketData
{
    static {
        PacketEvents.getAPI().getEventManager().registerListener(new LookPacketDataUpdater());
    }

    private final RingBuffer<RotationChange> rotationChangeQueue = new RingBuffer<>(20, new RotationChange(0, 0));

    public record ScaffoldAngleInfo(double angleChangeSum, double angleVariance, List<Double> angleList) {}

    private double[] getRecentAngleChanges()
    {
        // Get the rotation changes from the queue.
        final RotationChange[] changes;
        synchronized (this.rotationChangeQueue) {
            changes = this.rotationChangeQueue.toArray(new RotationChange[0]);
        }

        // Ignore if there are not enough changes.
        if (changes.length < 2) return new double[0];

        // Ignore if the last change is older than 1 second.
        final long curTime = System.currentTimeMillis();
        final double[] angles = new double[changes.length - 1];
        int index = 0;

        for (int i = 1; i < changes.length; ++i) {
            // Ignore rotation changes more than 1 second ago.
            if ((curTime - changes[i].getTime()) > 1000) continue;

            // Accumulate the angle change
            angles[index++] = changes[i - 1].angle(changes[i]);
        }
        // Only return the values that were actually written.
        return Arrays.copyOf(angles, index);
    }

    public Optional<ScaffoldAngleInfo> calculateRecentAngleStatistics()
    {
        final double[] angleArray = getRecentAngleChanges();
        if (angleArray.length == 0) return Optional.empty();

        final double angleSum = DataUtil.sum(angleArray);
        final double angleVariance = DataUtil.variance(angleSum / angleArray.length, angleArray);

        Log.finer(() -> "Scaffold-Debug | AngleSum: %.3f | AngleVariance: %.3f | Mean: %.3f | Max: %.3f".formatted(angleSum, angleVariance, angleSum / angleArray.length, Doubles.max(angleArray)));

        // Return the accumulated sum of angle changes and the gaps
        return Optional.of(new ScaffoldAngleInfo(angleSum, angleVariance, Arrays.stream(angleArray).boxed().toList()));
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
            return MathUtil.getAngleBetweenRotations(this.yaw, this.pitch, rotationChange.getYaw(), rotationChange.getPitch());
        }

        public long timeOffset(RotationChange other)
        {
            return MathUtil.absDiff(this.time, other.time);
        }

        public long tickOffset(RotationChange other)
        {
            return TimeUtil.toTicks(timeOffset(other));
        }
    }

    /**
     * Singleton class responsible for updating the {@link LookPacketData} based on received packets.
     * Reduces the number of required packet listeners.
     */
    private static final class LookPacketDataUpdater extends PacketListenerAbstract
    {
        public LookPacketDataUpdater()
        {
            super(PacketListenerPriority.MONITOR);
        }

        @Override
        public void onPacketReceive(PacketReceiveEvent event)
        {
            if (event.getPacketType() == PacketType.Play.Client.PLAYER_ROTATION ||
                event.getPacketType() == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {

                final var user = User.getUser(event);
                if (user == null) return;

                final var rotation = PacketEventUtils.getRotationFromEvent(event);

                final var rotationChange = new RotationChange(rotation.yaw(), rotation.pitch());
                final var rotationQueue = user.getLookPacketData().rotationChangeQueue;

                // Same tick -> merge
                synchronized (rotationQueue) {
                    if (rotationChange.timeOffset(rotationQueue.getLast()) < 55) rotationQueue.getLast().merge(rotationChange);
                    else rotationQueue.add(rotationChange);
                }

                // Huge angle change
                // Use the map values here to because the other ones are already updated.
                if (MathUtil.getAngleBetweenRotations(user.getData().floating.lastPacketYaw, user.getData().floating.lastPacketPitch, rotation.yaw(), rotation.pitch()) > ScaffoldRotation.SIGNIFICANT_ROTATION_CHANGE_THRESHOLD) {
                    user.getTimeMap().at(TimeKey.SCAFFOLD_SIGNIFICANT_ROTATION_CHANGE).update();
                }

                // Update the values here so the RotationUtil calculation is functional.
                user.getData().floating.lastPacketYaw = rotation.yaw();
                user.getData().floating.lastPacketPitch = rotation.pitch();
            }
        }
    }
}

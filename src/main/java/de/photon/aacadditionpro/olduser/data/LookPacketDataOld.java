package de.photon.aacadditionpro.olduser.data;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.photon.aacadditionpro.AACAdditionPro;
import de.photon.aacadditionpro.olduser.TimeDataOld;
import de.photon.aacadditionpro.user.UserManager;
import de.photon.aacadditionpro.olduser.UserOld;
import de.photon.aacadditionpro.util.mathematics.MathUtils;
import de.photon.aacadditionpro.util.mathematics.RotationUtil;
import de.photon.aacadditionpro.util.packetwrappers.client.IWrapperPlayClientLook;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedList;

public class LookPacketDataOld extends TimeDataOld
{
    private static final byte QUEUE_CAPACITY = 20;

    static {
        ProtocolLibrary.getProtocolManager().addPacketListener(new LookPacketDataOld.LookPacketDataUpdater());
    }

    // PacketAnalysisData
    @Getter
    private float realLastYaw;
    @Getter
    private float realLastPitch;

    @Getter
    private final Deque<RotationChange> rotationChangeQueue = new LinkedList<>();

    // [0] = Significant rotation changes (scaffold)
    public LookPacketDataOld(final UserOld user)
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
    public float[] getAngleInformation()
    {
        final float[] result = new float[2];

        // Ticks that must be added to fill up the gaps in the queue.
        short gapFillers = 0;

        synchronized (this.rotationChangeQueue) {
            final Collection<Float> rotationCache = new ArrayList<>(this.rotationChangeQueue.size());
            final RotationChange[] elementArray = this.rotationChangeQueue.toArray(new RotationChange[0]);


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
        public LookPacketDataUpdater()
        {
            super(AACAdditionPro.getInstance(), ListenerPriority.MONITOR, PacketType.Play.Client.LOOK, PacketType.Play.Client.POSITION_LOOK);
        }

        @Override
        public void onPacketReceiving(PacketEvent event)
        {
            // Not cancelled
            if (!event.isCancelled()) {
                final UserOld user = UserManager.getUser(event.getPlayer().getUniqueId());

                if (user == null) {
                    return;
                }

                final IWrapperPlayClientLook lookWrapper = event::getPacket;

                final RotationChange rotationChange = new RotationChange(lookWrapper.getYaw(), lookWrapper.getPitch());

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
                if (RotationUtil.getDirection(user.getLookPacketData().realLastYaw, user.getLookPacketData().realLastPitch).angle(RotationUtil.getDirection(lookWrapper.getYaw(), lookWrapper.getPitch())) > 35) {
                    user.getLookPacketData().updateTimeStamp(0);
                }

                // Update the values here so the RotationUtil calculation is functional.
                user.getLookPacketData().realLastYaw = lookWrapper.getYaw();
                user.getLookPacketData().realLastPitch = lookWrapper.getPitch();
            }
        }
    }
}

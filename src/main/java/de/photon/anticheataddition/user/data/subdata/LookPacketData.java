package de.photon.anticheataddition.user.data.subdata;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.google.common.collect.ImmutableList;
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
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Optional;

/**
 * Collects *per–tick* rotation deltas of a {@link User}.<br/>
 * <p>
 * The Minecraft client sends <i>absolute</i> yaw &amp; pitch values.<br/>
 * In order to reason about user behaviour we convert those absolutes into
 * <b>deltas</b> – the signed change between two consecutive packets.<br/>
 * Each tick (≤50ms) we accumulate all packet–local deltas into a single
 * {@link AngleDelta}. That drastically simplifies downstream processing while
 * still preserving the raw delta values via {@link #snapshot()}.
 * <p>
 * The last {@value #BUFFER_CAPACITY} accumulated deltas (≈1s @20TPS) are
 * retained in a ring‑buffer. Thread‑safety for multi‑threaded
 * listeners is achieved by synchronising on the buffer instance.
 */
public final class LookPacketData
{

    /* ---------- configuration ---------- */

    /** Maximum amount of per‑tick deltas kept in memory. */
    private static final int BUFFER_CAPACITY = 20;

    /** Time‑window used by {@link #calculateRecentAngleStatistics()} (ms). */
    private static final long DEFAULT_WINDOW_MS = 1_000L;

    /* ---------- storage ---------- */

    private final RingBuffer<AngleDelta> deltaBuffer = new RingBuffer<>(BUFFER_CAPACITY, AngleDelta.ZERO);

    /* ---------- public API ---------- */

    /**
     * An immutable view of all currently stored deltas (oldest → newest).
     * <p>
     * The returned list is safe to iterate over without further locking but
     * represents a <i>snapshot</i>; it is not updated after the call.
     */
    public List<AngleDelta> snapshot()
    {
        synchronized (deltaBuffer) {
            return ImmutableList.copyOf(deltaBuffer);
        }
    }

    /**
     * Computes aggregated statistics (sum, variance, list) of all rotation
     * magnitudes that occurred within the configured time‑window.
     */
    public Optional<ScaffoldAngleInfo> calculateRecentAngleStatistics()
    {
        final long now = System.currentTimeMillis();

        final double[] magnitudes;
        synchronized (deltaBuffer) {
            magnitudes = deltaBuffer.stream()
                                    .filter(d -> now - d.timestamp <= DEFAULT_WINDOW_MS)
                                    .mapToDouble(d -> d.magnitude)
                                    .toArray();
        }

        if (magnitudes.length == 0) return Optional.empty();

        final DoubleSummaryStatistics stats = Arrays.stream(magnitudes).summaryStatistics();
        final double variance = DataUtil.variance(stats.getAverage(), magnitudes);

        Log.finer(() -> "Scaffold-Debug | AngleSum: %.3f | AngleVariance: %.3f | Mean: %.3f | Max: %.3f".formatted(stats.getSum(), variance, stats.getAverage(), stats.getMax()));

        return Optional.of(new ScaffoldAngleInfo(stats.getSum(), variance, Doubles.asList(magnitudes)));
    }

    /* --------------------------------------------------------------------- */
    /*                               internal                               */
    /* --------------------------------------------------------------------- */

    /**
     * Single rotation delta accumulated per client tick.
     */
    @Value
    public static class AngleDelta
    {
        /** Time at which this delta was created (ms since epoch). */
        long timestamp = System.currentTimeMillis();

        @NonFinal float deltaYaw;
        @NonFinal float deltaPitch;
        @NonFinal double magnitude;

        /** Empty delta used as ring‑buffer placeholder. */
        static final AngleDelta ZERO = new AngleDelta(0f, 0f, 0d);

        private AngleDelta(float deltaYaw, float deltaPitch, double magnitude)
        {
            this.deltaYaw = deltaYaw;
            this.deltaPitch = deltaPitch;
            this.magnitude = magnitude;
        }

        /** Accumulates {@code other} into {@code this}. */
        void accumulate(AngleDelta other)
        {
            this.deltaYaw += other.deltaYaw;
            this.deltaPitch += other.deltaPitch;
            this.magnitude += other.magnitude;
        }
    }

    /**
     * Simple DTO returned by {@link #calculateRecentAngleStatistics()}.
     * <p>
     * The <i>raw</i> list of magnitudes is exposed for advanced downstream
     * analysis while still offering convenience fields.
     */
    public record ScaffoldAngleInfo(double deltaAngleChangeSum,
                                    double deltaAngleVariance,
                                    List<Double> deltaAngleMagnitudes)
    {
    }

    /* --------------------------------------------------------------------- */
    /*                               listener                               */
    /* --------------------------------------------------------------------- */

    /**
     * Dedicated, low‑overhead packet listener that converts absolute rotation
     * packets into deltas and feeds them into the owning {@link LookPacketData}
     * instance stored on each {@link User}.
     * <p>
     * One shared listener is cheaper than instantiating a listener per user.
     */
    private static final class LookPacketDataUpdater extends PacketListenerAbstract
    {

        LookPacketDataUpdater()
        {
            super(PacketListenerPriority.MONITOR);
        }

        @Override
        public void onPacketReceive(PacketReceiveEvent event)
        {
            if (event.getPacketType() != PacketType.Play.Client.PLAYER_ROTATION &&
                event.getPacketType() != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) return;

            final User user = User.getUser(event);
            if (user == null) return;

            final var rotation = PacketEventUtils.getRotationFromEvent(event);

            /* ----------- delta calculation ----------- */
            final float lastYaw = user.getData().floating.lastPacketYaw;
            final float lastPitch = user.getData().floating.lastPacketPitch;

            final float deltaYaw = (float) MathUtil.yawDistance(rotation.yaw(), lastYaw);
            final float deltaPitch = (float) MathUtil.absDiff(rotation.pitch(), lastPitch);
            final double magnitude = MathUtil.getAngleBetweenRotations(lastYaw, lastPitch, rotation.yaw(), rotation.pitch());

            final AngleDelta delta = new AngleDelta(deltaYaw, deltaPitch, magnitude);
            final RingBuffer<AngleDelta> buffer = user.getLookPacketData().deltaBuffer;

            /* ----------- store / merge ----------- */
            synchronized (buffer) {
                if (TimeUtil.toTicks(delta.timestamp - buffer.getLast().timestamp) <= 0) {
                    // merge into same tick
                    buffer.getLast().accumulate(delta);
                } else {
                    buffer.add(delta);
                }
            }

            /* ----------- flag large spikes ----------- */
            if (deltaYaw > ScaffoldRotation.SIGNIFICANT_YAW_CHANGE) {
                user.getTimeMap().at(TimeKey.SCAFFOLD_SIGNIFICANT_YAW_CHANGE).update();
            }

            /* ----------- update absolute orientation ----------- */
            user.getData().floating.lastPacketYaw = rotation.yaw();
            user.getData().floating.lastPacketPitch = rotation.pitch();
        }
    }

    static {
        PacketEvents.getAPI().getEventManager().registerListener(new LookPacketDataUpdater());
    }
}

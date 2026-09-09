package de.photon.anticheataddition.user.data.subdata;

import de.photon.anticheataddition.modules.checks.targeting.TargetingAnalysis;
import de.photon.anticheataddition.modules.checks.targeting.TargetingContext;
import de.photon.anticheataddition.modules.checks.targeting.TargetingMixedAnalysis;

import java.util.Arrays;
import java.util.Optional;

/**
 * Per-user movement-packet rotation history shared by the Targeting submodules.
 *
 * <p>Every accepted movement packet is represented. Packets which omit yaw and pitch repeat the most recently known
 * rotation, which is important because a perfectly fixed targeting angle would otherwise produce no samples at all.
 * Packet gaps and abrupt rotations deliberately do not clear the history because either condition can be produced by a
 * client to poison a statistical window.</p>
 *
 * <p>The supplied timestamps are based on a monotonic clock and are used only for elapsed-time diagnostics. Packet
 * order and sequence numbers are the source of truth for analysis, so delaying packets cannot reset or exempt a
 * player.</p>
 */
public final class TargetingData
{
    private static final int BUFFER_CAPACITY = 96;
    private static final int ANALYSIS_SAMPLE_COUNT = 48;
    private static final int ACQUISITION_SAMPLE_COUNT = 24;
    private static final int MINIMUM_ACQUISITION_SAMPLE_COUNT = 8;
    private static final int MINIMUM_NEW_SAMPLES = 8;
    private static final long TRUSTED_BOUNDARY_SUPPRESSION_NANOS = 500_000_000L;

    private static final double MINIMUM_VALID_PITCH = -90D;
    private static final double MAXIMUM_VALID_PITCH = 90D;

    private final double[] x = new double[BUFFER_CAPACITY];
    private final double[] y = new double[BUFFER_CAPACITY];
    private final double[] z = new double[BUFFER_CAPACITY];
    private final double[] yaw = new double[BUFFER_CAPACITY];
    private final double[] pitch = new double[BUFFER_CAPACITY];
    private final long[] timestamp = new long[BUFFER_CAPACITY];
    private final long[] sampleSequence = new long[BUFFER_CAPACITY];
    private final boolean[] trustedBreakBefore = new boolean[BUFFER_CAPACITY];
    private final int[][] mixedModeHistory = new int[TargetingContext.values().length][TargetingMixedAnalysis.HISTORY_LENGTH];
    private final int[] mixedModeWriteIndex = new int[TargetingContext.values().length];
    private final int[] mixedModeSize = new int[TargetingContext.values().length];

    private int writeIndex;
    private int size;
    private long sequence;
    private long lastAnalyzedSequence;
    private long lastAcquisitionSequence;
    private double lastX;
    private double lastY;
    private double lastZ;
    private double lastYaw;
    private double lastPitch;
    private boolean hasLastPosition;
    private boolean hasLastRotation;
    private boolean trustedBoundaryPending;
    private long targetingSuppressedUntil;

    /**
     * Adds a movement packet while retaining omitted position or rotation components from the previous packet.
     *
     * <p>Malformed finite-state components do not stall Targeting collection. Once a legal value is known, an invalid
     * component retains the last legal value while ACA's dedicated packet-validity checks inspect the original packet.</p>
     *
     * @return whether the sample was accepted
     */
    public synchronized RotationUpdate addMovement(final double currentX, final double currentY, final double currentZ, final double currentYaw, final double currentPitch, final boolean positionChanged, final boolean rotationChanged, final long currentTimestamp)
    {
        final boolean validX = Double.isFinite(currentX);
        final boolean validY = Double.isFinite(currentY);
        final boolean validZ = Double.isFinite(currentZ);
        final boolean validYaw = Double.isFinite(currentYaw);
        final boolean validPitch = Double.isFinite(currentPitch) && currentPitch >= MINIMUM_VALID_PITCH && currentPitch <= MAXIMUM_VALID_PITCH;

        // Position and look may arrive in separate packet variants. Retain each complete component group
        // during initialization, but do not invent a statistical sample until both groups are known.
        if (positionChanged && (hasLastPosition || validX && validY && validZ)) {
            if (validX) lastX = currentX;
            if (validY) lastY = currentY;
            if (validZ) lastZ = currentZ;
            hasLastPosition = true;
        }
        if (rotationChanged && (hasLastRotation || validYaw && validPitch)) {
            if (validYaw) lastYaw = TargetingAnalysis.normalizeYaw(currentYaw);
            if (validPitch) lastPitch = currentPitch;
            hasLastRotation = true;
        }
        if (!hasLastPosition || !hasLastRotation) return new RotationUpdate(false);
        return addAcceptedMovement(lastX, lastY, lastZ, lastYaw, lastPitch, currentTimestamp);
    }

    /**
     * Adds a movement packet which explicitly contains yaw and pitch but no new position.
     *
     * <p>This compatibility method is retained for existing callers and tests. Production packet collection should use
     * {@link #addMovement(double, double, double, double, double, boolean, boolean, long)} so Acquisition receives the
     * packet-order position history as well.</p>
     */
    public synchronized RotationUpdate addRotation(final double currentYaw, final double currentPitch, final long currentTimestamp)
    {
        return addMovement(hasLastPosition ? lastX : 0D, hasLastPosition ? lastY : 0D, hasLastPosition ? lastZ : 0D, currentYaw, currentPitch, !hasLastPosition, true, currentTimestamp);
    }

    /**
     * Adds a movement packet which omitted both position and rotation.
     */
    public synchronized RotationUpdate addUnchangedRotation(final long currentTimestamp)
    {
        return addMovement(lastX, lastY, lastZ, lastYaw, lastPitch, false, false, currentTimestamp);
    }


    /**
     * Updates the server-authoritative position and rotation without inserting a client statistical sample.
     *
     * <p>The next real movement packet receives a trusted boundary. Earlier samples, replay fingerprints, and violation
     * evidence remain intact, while the server-generated teleport transition is excluded from path analyses.</p>
     */
    public synchronized RotationUpdate addTrustedMovement(final double currentX, final double currentY, final double currentZ, final double currentYaw, final double currentPitch, final long currentTimestamp)
    {
        if (!Double.isFinite(currentX) || !Double.isFinite(currentY) || !Double.isFinite(currentZ) || !Double.isFinite(currentYaw) || !Double.isFinite(currentPitch) || currentPitch < MINIMUM_VALID_PITCH || currentPitch > MAXIMUM_VALID_PITCH)
            return new RotationUpdate(false);

        lastX = currentX;
        lastY = currentY;
        lastZ = currentZ;
        lastYaw = TargetingAnalysis.normalizeYaw(currentYaw);
        lastPitch = currentPitch;
        hasLastPosition = true;
        hasLastRotation = true;
        trustedBoundaryPending = size > 0;
        targetingSuppressedUntil = Math.max(targetingSuppressedUntil, currentTimestamp + TRUSTED_BOUNDARY_SUPPRESSION_NANOS);
        return new RotationUpdate(true);
    }

    /**
     * Updates only the server-authoritative rotation. Prefer {@link #addTrustedMovement(double, double, double, double,
     * double, long)} for teleports so the packet-order position history stays aligned as well.
     */
    public synchronized RotationUpdate addTrustedRotation(final double currentYaw, final double currentPitch, final long currentTimestamp)
    {
        if (!hasLastPosition) return new RotationUpdate(false);
        return addTrustedMovement(lastX, lastY, lastZ, currentYaw, currentPitch, currentTimestamp);
    }

    /**
     * Returns the most recent packet-order movement history for target-relative successful-hit analysis.
     *
     * <p>Only samples after the latest trusted boundary and previous acquisition snapshot are returned. At least eight
     * fresh samples are required, so repeated damage cannot count overlapping approaches as independent evidence.</p>
     */
    public synchronized Optional<AcquisitionSnapshot> takeAcquisitionSnapshot()
    {
        return takeAcquisitionSnapshot(System.nanoTime());
    }

    /**
     * Takes an acquisition snapshot using a caller-supplied monotonic timestamp.
     *
     * <p>The overload keeps lifecycle suppression deterministic for packet/event code and tests while the no-argument
     * method remains convenient for callers which do not already have a timestamp.</p>
     */
    public synchronized Optional<AcquisitionSnapshot> takeAcquisitionSnapshot(final long currentTimestamp)
    {
        if (trustedBoundaryPending || currentTimestamp < targetingSuppressedUntil || size < MINIMUM_ACQUISITION_SAMPLE_COUNT || sequence == lastAcquisitionSequence || !hasLastPosition || !hasLastRotation) return Optional.empty();

        int segmentStart = 0;
        for (int logicalIndex = size - 1; logicalIndex >= 0; logicalIndex--) {
            final int physicalIndex = physicalIndex(logicalIndex);
            if (trustedBreakBefore[physicalIndex]) {
                segmentStart = logicalIndex;
                break;
            }
        }

        final int segmentSize = size - segmentStart;
        if (segmentSize < MINIMUM_ACQUISITION_SAMPLE_COUNT) return Optional.empty();
        // Profiles are evidence across independent acquisitions. Reusing all but one packet from the previous hit
        // would count the same slowdown repeatedly, even when the only new packet repeats the final rotation.
        final int sampleCount = (int) Math.min(Math.min(segmentSize, ACQUISITION_SAMPLE_COUNT),
                                               sequence - lastAcquisitionSequence);
        if (sampleCount < MINIMUM_ACQUISITION_SAMPLE_COUNT) return Optional.empty();
        final int firstLogicalIndex = size - sampleCount;
        final double[] xSnapshot = new double[sampleCount];
        final double[] ySnapshot = new double[sampleCount];
        final double[] zSnapshot = new double[sampleCount];
        final double[] yawSnapshot = new double[sampleCount];
        final double[] pitchSnapshot = new double[sampleCount];
        final long[] sequenceSnapshot = new long[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            final int physicalIndex = physicalIndex(firstLogicalIndex + i);
            xSnapshot[i] = x[physicalIndex];
            ySnapshot[i] = y[physicalIndex];
            zSnapshot[i] = z[physicalIndex];
            yawSnapshot[i] = yaw[physicalIndex];
            pitchSnapshot[i] = pitch[physicalIndex];
            sequenceSnapshot[i] = sampleSequence[physicalIndex];
        }

        lastAcquisitionSequence = sequence;
        return Optional.of(new AcquisitionSnapshot(xSnapshot, ySnapshot, zSnapshot, yawSnapshot, pitchSnapshot, sequenceSnapshot));
    }

    /**
     * Returns the most recent complete analysis window when enough new movement packets have arrived.
     *
     * <p>No maximum packet gap or maximum sample age is used. A client therefore cannot erase or indefinitely postpone
     * analysis by inserting an artificial lag spike. The window always consists of the latest packet-ordered samples.</p>
     */
    public synchronized Optional<Snapshot> takeSnapshot()
    {
        if (size < TargetingAnalysis.MINIMUM_SAMPLE_COUNT || sequence - lastAnalyzedSequence < MINIMUM_NEW_SAMPLES) return Optional.empty();

        final int sampleCount = Math.min(size, ANALYSIS_SAMPLE_COUNT);
        final int firstLogicalIndex = size - sampleCount;
        final double[] yawSnapshot = new double[sampleCount];
        final double[] pitchSnapshot = new double[sampleCount];
        final boolean[] trustedBreakSnapshot = new boolean[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            final int physicalIndex = physicalIndex(firstLogicalIndex + i);
            yawSnapshot[i] = yaw[physicalIndex];
            pitchSnapshot[i] = pitch[physicalIndex];
            trustedBreakSnapshot[i] = trustedBreakBefore[physicalIndex];
        }

        final int firstPhysicalIndex = physicalIndex(firstLogicalIndex);
        final int lastPhysicalIndex = physicalIndex(size - 1);
        lastAnalyzedSequence = sequence;
        return Optional.of(new Snapshot(yawSnapshot, pitchSnapshot, trustedBreakSnapshot, sampleSequence[firstPhysicalIndex], sampleSequence[lastPhysicalIndex]));
    }

    /**
     * Stores one complete-window suspicious-mode observation for the supplied interaction context.
     *
     * <p>This history is intentionally not cleared by {@link #clear()}. A server-confirmed teleport may break rotation
     * continuity, but it must not allow a client to erase evidence accumulated by alternating targeting strategies.</p>
     *
     * @return a chronological copy of the retained mode-mask history
     */
    public synchronized int[] addMixedModeObservation(final TargetingContext context, final int modeMask)
    {
        if (context == null) throw new NullPointerException("context must not be null");
        final int contextIndex = context.ordinal();
        final int[] history = mixedModeHistory[contextIndex];
        history[mixedModeWriteIndex[contextIndex]] = modeMask;
        mixedModeWriteIndex[contextIndex] = (mixedModeWriteIndex[contextIndex] + 1) % history.length;
        if (mixedModeSize[contextIndex] < history.length) mixedModeSize[contextIndex]++;

        final int retained = mixedModeSize[contextIndex];
        final int[] snapshot = new int[retained];
        final int oldest = (mixedModeWriteIndex[contextIndex] - retained + history.length) % history.length;
        for (int i = 0; i < retained; i++) snapshot[i] = history[(oldest + i) % history.length];
        return snapshot;
    }

    /**
     * Clears only the current packet history. Ordinary teleports should use {@link #addTrustedRotation(double, double,
     * long)} instead, because clearing can otherwise become an attacker-controlled sample-reset primitive.
     */
    public synchronized void clear()
    {
        writeIndex = 0;
        size = 0;
        lastAnalyzedSequence = sequence;
        lastAcquisitionSequence = sequence;
        lastX = 0D;
        lastY = 0D;
        lastZ = 0D;
        lastYaw = 0D;
        lastPitch = 0D;
        hasLastPosition = false;
        hasLastRotation = false;
        trustedBoundaryPending = false;
        targetingSuppressedUntil = 0L;
        Arrays.fill(trustedBreakBefore, false);
    }

    /**
     * Returns whether a server-authoritative look change is waiting to be followed by a real client movement packet.
     * Interaction analysis is postponed in this short state because the retained packet history still belongs to the
     * pre-teleport camera context.
     */
    public synchronized boolean hasPendingTrustedBoundary()
    {
        return trustedBoundaryPending;
    }

    /**
     * Returns whether targeting analysis is temporarily suppressed after a server-controlled context change.
     */
    public synchronized boolean isTargetingSuppressed(final long currentTimestamp)
    {
        return currentTimestamp < targetingSuppressedUntil;
    }

    /**
     * Finds the accepted movement rotation closest to the supplied timestamp.
     */
    public synchronized Optional<RotationSample> nearestRotation(final long currentTimestamp, final long maximumDifferenceNanos)
    {
        RotationSample nearest = null;
        long nearestDifference = Long.MAX_VALUE;
        for (int logicalIndex = size - 1; logicalIndex >= 0; logicalIndex--) {
            final int physicalIndex = physicalIndex(logicalIndex);
            final long difference = Math.abs(timestamp[physicalIndex] - currentTimestamp);
            if (difference < nearestDifference) {
                nearestDifference = difference;
                nearest = new RotationSample(yaw[physicalIndex], pitch[physicalIndex], timestamp[physicalIndex]);
            }
        }
        return nearest == null || nearestDifference > maximumDifferenceNanos ? Optional.empty() : Optional.of(nearest);
    }

    /**
     * @return the current number of retained movement-packet rotation samples
     */
    public synchronized int size()
    {
        return size;
    }

    private RotationUpdate addAcceptedMovement(final double currentX, final double currentY, final double currentZ, final double currentYaw, final double currentPitch, final long currentTimestamp)
    {
        final boolean trustedBoundary = trustedBoundaryPending && size > 0;
        trustedBoundaryPending = false;

        sequence++;
        x[writeIndex] = currentX;
        y[writeIndex] = currentY;
        z[writeIndex] = currentZ;
        yaw[writeIndex] = currentYaw;
        pitch[writeIndex] = currentPitch;
        timestamp[writeIndex] = currentTimestamp;
        sampleSequence[writeIndex] = sequence;
        trustedBreakBefore[writeIndex] = trustedBoundary;
        writeIndex = (writeIndex + 1) % BUFFER_CAPACITY;
        if (size < BUFFER_CAPACITY) size++;

        lastX = currentX;
        lastY = currentY;
        lastZ = currentZ;
        lastYaw = currentYaw;
        lastPitch = currentPitch;
        hasLastPosition = true;
        hasLastRotation = true;
        return new RotationUpdate(true);
    }

    private int physicalIndex(final int logicalIndex)
    {
        final int oldestIndex = (writeIndex - size + BUFFER_CAPACITY) % BUFFER_CAPACITY;
        return (oldestIndex + logicalIndex) % BUFFER_CAPACITY;
    }

    /**
     * Result of adding one movement-packet rotation sample.
     */
    public record RotationUpdate(boolean accepted)
    {}

    /**
     * One accepted movement rotation and its monotonic packet timestamp.
     */
    public record RotationSample(double yaw, double pitch, long timestamp)
    {}

    /**
     * Immutable packet-ordered snapshot used by the shared statistical analysis.
     */
    public record Snapshot(double[] yaw, double[] pitch, boolean[] trustedBreakBefore, long firstSequence, long lastSequence)
    {

        public Snapshot
        {
            yaw = Arrays.copyOf(yaw, yaw.length);
            pitch = Arrays.copyOf(pitch, pitch.length);
            trustedBreakBefore = Arrays.copyOf(trustedBreakBefore, trustedBreakBefore.length);
            if (yaw.length != pitch.length || yaw.length != trustedBreakBefore.length) {
                throw new IllegalArgumentException("snapshot arrays must have the same length");
            }
        }

        @Override
        public double[] yaw()
        {
            return Arrays.copyOf(yaw, yaw.length);
        }

        @Override
        public double[] pitch()
        {
            return Arrays.copyOf(pitch, pitch.length);
        }

        @Override
        public boolean[] trustedBreakBefore()
        {
            return Arrays.copyOf(trustedBreakBefore, trustedBreakBefore.length);
        }
    }

    /**
     * Immutable packet-order position and rotation history for one successful-hit acquisition.
     */
    public record AcquisitionSnapshot(double[] x, double[] y, double[] z, double[] yaw, double[] pitch, long[] sequence)
    {
        public AcquisitionSnapshot
        {
            x = Arrays.copyOf(x, x.length);
            y = Arrays.copyOf(y, y.length);
            z = Arrays.copyOf(z, z.length);
            yaw = Arrays.copyOf(yaw, yaw.length);
            pitch = Arrays.copyOf(pitch, pitch.length);
            sequence = Arrays.copyOf(sequence, sequence.length);
            if (x.length != y.length || x.length != z.length || x.length != yaw.length || x.length != pitch.length || x.length != sequence.length) {
                throw new IllegalArgumentException("acquisition snapshot arrays must have the same length");
            }
        }

        @Override
        public double[] x()
        {
            return Arrays.copyOf(x, x.length);
        }

        @Override
        public double[] y()
        {
            return Arrays.copyOf(y, y.length);
        }

        @Override
        public double[] z()
        {
            return Arrays.copyOf(z, z.length);
        }

        @Override
        public double[] yaw()
        {
            return Arrays.copyOf(yaw, yaw.length);
        }

        @Override
        public double[] pitch()
        {
            return Arrays.copyOf(pitch, pitch.length);
        }

        @Override
        public long[] sequence()
        {
            return Arrays.copyOf(sequence, sequence.length);
        }
    }

}

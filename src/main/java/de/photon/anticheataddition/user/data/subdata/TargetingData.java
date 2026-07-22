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
public final class TargetingData {
    private static final int BUFFER_CAPACITY = 96;
    private static final int ANALYSIS_SAMPLE_COUNT = 48;
    private static final int INTERACTION_SAMPLE_COUNT = 16;
    private static final int ACQUISITION_SAMPLE_COUNT = 24;
    private static final int MINIMUM_ACQUISITION_SAMPLE_COUNT = 8;
    private static final int MINIMUM_NEW_SAMPLES = 8;

    private static final double MINIMUM_VALID_PITCH = -90D;
    private static final double MAXIMUM_VALID_PITCH = 90D;

    private static final double MINIMUM_YAW_SNAP = 0.45D;
    private static final double MINIMUM_PITCH_SNAP = 0.35D;
    private static final double MINIMUM_RETURN_RATIO = 0.8D;
    private static final double MINIMUM_RETURN_CANCELLATION = 0.75D;
    private static final double MINIMUM_RETURN_PATH_EFFICIENCY = 0.65D;

    private final double[] x = new double[BUFFER_CAPACITY];
    private final double[] y = new double[BUFFER_CAPACITY];
    private final double[] z = new double[BUFFER_CAPACITY];
    private final double[] yaw = new double[BUFFER_CAPACITY];
    private final double[] pitch = new double[BUFFER_CAPACITY];
    private final long[] timestamp = new long[BUFFER_CAPACITY];
    private final long[] sampleSequence = new long[BUFFER_CAPACITY];
    private final boolean[] trustedBreakBefore = new boolean[BUFFER_CAPACITY];
    private final int[][] mixedModeHistory =
            new int[TargetingContext.values().length][TargetingMixedAnalysis.HISTORY_LENGTH];
    private final int[] mixedModeWriteIndex = new int[TargetingContext.values().length];
    private final int[] mixedModeSize = new int[TargetingContext.values().length];

    private int writeIndex;
    private int size;
    private long sequence;
    private long lastAnalyzedSequence;
    private long lastInteractionSequence;
    private long lastAcquisitionSequence;
    private double lastX;
    private double lastY;
    private double lastZ;
    private double lastYaw;
    private double lastPitch;
    private boolean hasLastPosition;
    private boolean hasLastRotation;
    private boolean trustedBoundaryPending;

    private boolean pendingInteraction;
    private TargetingContext pendingContext;
    private double pendingBeforeYaw;
    private double pendingBeforePitch;
    private double pendingInteractionYaw;
    private double pendingInteractionPitch;
    private double pendingLastYaw;
    private double pendingLastPitch;
    private double pendingYawPathLength;
    private double pendingPitchPathLength;
    private long pendingInteractionTimestamp;
    private int pendingFollowingPackets;

    /**
     * Adds a movement packet while retaining omitted position or rotation components from the previous packet.
     *
     * <p>Malformed finite-state components do not stall Targeting collection. Once a legal value is known, an invalid
     * component retains the last legal value while ACA's dedicated packet-validity checks inspect the original packet.</p>
     *
     * @return whether the sample was accepted and, when available, a completed snap-back sample
     */
    public synchronized RotationUpdate addMovement(final double currentX,
                                                   final double currentY,
                                                   final double currentZ,
                                                   final double currentYaw,
                                                   final double currentPitch,
                                                   final boolean positionChanged,
                                                   final boolean rotationChanged,
                                                   final long currentTimestamp)
    {
        if (!hasLastPosition && !positionChanged) return new RotationUpdate(false, null);
        if (!hasLastRotation && !rotationChanged) return new RotationUpdate(false, null);

        final boolean validX = Double.isFinite(currentX);
        final boolean validY = Double.isFinite(currentY);
        final boolean validZ = Double.isFinite(currentZ);
        final boolean validYaw = Double.isFinite(currentYaw);
        final boolean validPitch = Double.isFinite(currentPitch) &&
                                   currentPitch >= MINIMUM_VALID_PITCH &&
                                   currentPitch <= MAXIMUM_VALID_PITCH;

        if (positionChanged && (!validX || !validY || !validZ) && !hasLastPosition) {
            return new RotationUpdate(false, null);
        }
        if (rotationChanged && (!validYaw || !validPitch) && !hasLastRotation) {
            return new RotationUpdate(false, null);
        }

        final double acceptedX = positionChanged && validX ? currentX : lastX;
        final double acceptedY = positionChanged && validY ? currentY : lastY;
        final double acceptedZ = positionChanged && validZ ? currentZ : lastZ;
        final double acceptedYaw = rotationChanged && validYaw
                                   ? TargetingAnalysis.normalizeYaw(currentYaw)
                                   : lastYaw;
        final double acceptedPitch = rotationChanged && validPitch ? currentPitch : lastPitch;
        return addAcceptedMovement(acceptedX,
                                   acceptedY,
                                   acceptedZ,
                                   acceptedYaw,
                                   acceptedPitch,
                                   currentTimestamp);
    }

    /**
     * Adds a movement packet which explicitly contains yaw and pitch but no new position.
     *
     * <p>This compatibility method is retained for existing callers and tests. Production packet collection should use
     * {@link #addMovement(double, double, double, double, double, boolean, boolean, long)} so Acquisition receives the
     * packet-order position history as well.</p>
     */
    public synchronized RotationUpdate addRotation(final double currentYaw,
                                                   final double currentPitch,
                                                   final long currentTimestamp)
    {
        return addMovement(hasLastPosition ? lastX : 0D,
                           hasLastPosition ? lastY : 0D,
                           hasLastPosition ? lastZ : 0D,
                           currentYaw,
                           currentPitch,
                           !hasLastPosition,
                           true,
                           currentTimestamp);
    }

    /**
     * Adds a movement packet which omitted both position and rotation.
     */
    public synchronized RotationUpdate addUnchangedRotation(final long currentTimestamp)
    {
        return addMovement(lastX,
                           lastY,
                           lastZ,
                           lastYaw,
                           lastPitch,
                           false,
                           false,
                           currentTimestamp);
    }


    /**
     * Updates the server-authoritative position and rotation without inserting a client statistical sample.
     *
     * <p>The next real movement packet receives a trusted boundary. Earlier samples, replay fingerprints, and violation
     * evidence remain intact, while the server-generated teleport transition is excluded from path analyses.</p>
     */
    public synchronized RotationUpdate addTrustedMovement(final double currentX,
                                                          final double currentY,
                                                          final double currentZ,
                                                          final double currentYaw,
                                                          final double currentPitch,
                                                          final long currentTimestamp)
    {
        if (!Double.isFinite(currentX) ||
            !Double.isFinite(currentY) ||
            !Double.isFinite(currentZ) ||
            !Double.isFinite(currentYaw) ||
            !Double.isFinite(currentPitch) ||
            currentPitch < MINIMUM_VALID_PITCH ||
            currentPitch > MAXIMUM_VALID_PITCH) return new RotationUpdate(false, null);

        clearPendingInteraction();
        lastX = currentX;
        lastY = currentY;
        lastZ = currentZ;
        lastYaw = TargetingAnalysis.normalizeYaw(currentYaw);
        lastPitch = currentPitch;
        hasLastPosition = true;
        hasLastRotation = true;
        trustedBoundaryPending = size > 0;
        return new RotationUpdate(true, null);
    }

    /**
     * Updates only the server-authoritative rotation. Prefer {@link #addTrustedMovement(double, double, double, double,
     * double, long)} for teleports so the packet-order position history stays aligned as well.
     */
    public synchronized RotationUpdate addTrustedRotation(final double currentYaw,
                                                          final double currentPitch,
                                                          final long currentTimestamp)
    {
        if (!hasLastPosition) return new RotationUpdate(false, null);
        return addTrustedMovement(lastX, lastY, lastZ, currentYaw, currentPitch, currentTimestamp);
    }


    /**
     * Marks an attack or scaffold placement and returns the recent interaction-centered rotation history.
     *
     * <p>Repeated interactions without a new movement packet do not create duplicate reversal samples. A still-useful
     * pending snap-back cycle is not overwritten by attack spam. Once the intervening path has become too indirect to
     * represent an interaction-only restoration, the new interaction becomes the reference instead.</p>
     */
    public synchronized Optional<InteractionSnapshot> markInteraction(final TargetingContext context,
                                                                      final long currentTimestamp)
    {
        if (context == null) throw new NullPointerException("context must not be null");
        if (size < 2 || sequence == lastInteractionSequence) return Optional.empty();
        lastInteractionSequence = sequence;

        if (!pendingInteraction || pendingPathIsTooIndirect()) prepareSnapBack(context, currentTimestamp);

        final int sampleCount = Math.min(size, INTERACTION_SAMPLE_COUNT);
        final int firstLogicalIndex = size - sampleCount;
        final double[] yawSnapshot = new double[sampleCount];
        final double[] pitchSnapshot = new double[sampleCount];
        final long[] timestampSnapshot = new long[sampleCount];
        final long[] sequenceSnapshot = new long[sampleCount];
        final boolean[] trustedBreakSnapshot = new boolean[sampleCount];
        for (int i = 0; i < sampleCount; i++) {
            final int physicalIndex = physicalIndex(firstLogicalIndex + i);
            yawSnapshot[i] = yaw[physicalIndex];
            pitchSnapshot[i] = pitch[physicalIndex];
            timestampSnapshot[i] = timestamp[physicalIndex];
            sequenceSnapshot[i] = sampleSequence[physicalIndex];
            trustedBreakSnapshot[i] = trustedBreakBefore[physicalIndex];
        }

        return Optional.of(new InteractionSnapshot(context,
                                                   yawSnapshot,
                                                   pitchSnapshot,
                                                   timestampSnapshot,
                                                   sequenceSnapshot,
                                                   trustedBreakSnapshot,
                                                   currentTimestamp));
    }

    /**
     * Returns the most recent packet-order movement history for target-relative successful-hit analysis.
     *
     * <p>Only samples after the latest trusted boundary are returned. Repeated damage events without a new movement
     * packet cannot manufacture duplicate acquisitions.</p>
     */
    public synchronized Optional<AcquisitionSnapshot> takeAcquisitionSnapshot()
    {
        if (trustedBoundaryPending ||
            size < MINIMUM_ACQUISITION_SAMPLE_COUNT ||
            sequence == lastAcquisitionSequence ||
            !hasLastPosition ||
            !hasLastRotation) return Optional.empty();

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
        final int sampleCount = Math.min(segmentSize, ACQUISITION_SAMPLE_COUNT);
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
        return Optional.of(new AcquisitionSnapshot(xSnapshot,
                                                   ySnapshot,
                                                   zSnapshot,
                                                   yawSnapshot,
                                                   pitchSnapshot,
                                                   sequenceSnapshot));
    }

    /**
     * Returns the most recent complete analysis window when enough new movement packets have arrived.
     *
     * <p>No maximum packet gap or maximum sample age is used. A client therefore cannot erase or indefinitely postpone
     * analysis by inserting an artificial lag spike. The window always consists of the latest packet-ordered samples.</p>
     */
    public synchronized Optional<Snapshot> takeSnapshot()
    {
        if (size < TargetingAnalysis.MINIMUM_SAMPLE_COUNT ||
            sequence - lastAnalyzedSequence < MINIMUM_NEW_SAMPLES) return Optional.empty();

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
        return Optional.of(new Snapshot(yawSnapshot,
                                        pitchSnapshot,
                                        trustedBreakSnapshot,
                                        sampleSequence[firstPhysicalIndex],
                                        sampleSequence[lastPhysicalIndex]));
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
        lastInteractionSequence = sequence;
        lastAcquisitionSequence = sequence;
        lastX = 0D;
        lastY = 0D;
        lastZ = 0D;
        lastYaw = 0D;
        lastPitch = 0D;
        hasLastPosition = false;
        hasLastRotation = false;
        trustedBoundaryPending = false;
        Arrays.fill(trustedBreakBefore, false);
        clearPendingInteraction();
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
     * @return the current number of retained movement-packet rotation samples
     */
    public synchronized int size()
    {
        return size;
    }

    private RotationUpdate addAcceptedMovement(final double currentX,
                                               final double currentY,
                                               final double currentZ,
                                               final double currentYaw,
                                               final double currentPitch,
                                               final long currentTimestamp)
    {
        final boolean trustedBoundary = trustedBoundaryPending && size > 0;
        trustedBoundaryPending = false;
        final SnapBackSample snapBackSample = trustedBoundary
                                              ? null
                                              : completeSnapBack(currentYaw, currentPitch, currentTimestamp);

        sequence++;
        x[writeIndex] = currentX;
        y[writeIndex] = currentY;
        z[writeIndex] = currentZ;
        yaw[writeIndex] = currentYaw;
        pitch[writeIndex] = currentPitch;
        timestamp[writeIndex] = currentTimestamp;
        sampleSequence[writeIndex] = sequence;
        trustedBreakBefore[writeIndex] = trustedBoundary && size > 0;
        writeIndex = (writeIndex + 1) % BUFFER_CAPACITY;
        if (size < BUFFER_CAPACITY) size++;

        lastX = currentX;
        lastY = currentY;
        lastZ = currentZ;
        lastYaw = currentYaw;
        lastPitch = currentPitch;
        hasLastPosition = true;
        hasLastRotation = true;
        return new RotationUpdate(true, snapBackSample);
    }

    private void prepareSnapBack(final TargetingContext context, final long currentTimestamp)
    {
        final int interactionIndex = physicalIndex(size - 1);
        int bestBeforeIndex = physicalIndex(size - 2);
        double bestDistanceScore = -1D;
        final int maximumLookback = Math.min(INTERACTION_SAMPLE_COUNT - 1, size - 1);

        for (int lookback = 1; lookback <= maximumLookback; lookback++) {
            final int crossedSampleIndex = physicalIndex(size - lookback);
            if (trustedBreakBefore[crossedSampleIndex]) break;

            final int candidateIndex = physicalIndex(size - 1 - lookback);
            final double yawDistance = Math.abs(TargetingAnalysis.signedYawDelta(yaw[interactionIndex], yaw[candidateIndex]));
            final double pitchDistance = Math.abs(pitch[interactionIndex] - pitch[candidateIndex]);
            final double distanceScore = yawDistance / MINIMUM_YAW_SNAP + pitchDistance / MINIMUM_PITCH_SNAP;
            if (distanceScore > bestDistanceScore) {
                bestDistanceScore = distanceScore;
                bestBeforeIndex = candidateIndex;
            }
        }

        pendingContext = context;
        pendingBeforeYaw = yaw[bestBeforeIndex];
        pendingBeforePitch = pitch[bestBeforeIndex];
        pendingInteractionYaw = yaw[interactionIndex];
        pendingInteractionPitch = pitch[interactionIndex];
        pendingLastYaw = pendingInteractionYaw;
        pendingLastPitch = pendingInteractionPitch;
        pendingYawPathLength = 0D;
        pendingPitchPathLength = 0D;
        pendingInteractionTimestamp = currentTimestamp;
        pendingFollowingPackets = 0;
        pendingInteraction = true;
    }

    private SnapBackSample completeSnapBack(final double currentYaw,
                                            final double currentPitch,
                                            final long currentTimestamp)
    {
        if (!pendingInteraction) return null;
        pendingFollowingPackets++;

        pendingYawPathLength += Math.abs(TargetingAnalysis.signedYawDelta(currentYaw, pendingLastYaw));
        pendingPitchPathLength += Math.abs(currentPitch - pendingLastPitch);
        pendingLastYaw = currentYaw;
        pendingLastPitch = currentPitch;

        final double yawSnap = TargetingAnalysis.signedYawDelta(pendingInteractionYaw, pendingBeforeYaw);
        final double pitchSnap = pendingInteractionPitch - pendingBeforePitch;
        final double yawReturn = TargetingAnalysis.signedYawDelta(currentYaw, pendingInteractionYaw);
        final double pitchReturn = currentPitch - pendingInteractionPitch;
        final double yawReturnError = Math.abs(TargetingAnalysis.signedYawDelta(currentYaw, pendingBeforeYaw));
        final double pitchReturnError = Math.abs(currentPitch - pendingBeforePitch);

        final boolean suspiciousYaw = isSnapBackAxis(yawSnap,
                                                     yawReturn,
                                                     yawReturnError,
                                                     pendingYawPathLength,
                                                     MINIMUM_YAW_SNAP);
        final boolean suspiciousPitch = isSnapBackAxis(pitchSnap,
                                                       pitchReturn,
                                                       pitchReturnError,
                                                       pendingPitchPathLength,
                                                       MINIMUM_PITCH_SNAP);
        final int suspiciousAxes = (suspiciousYaw ? 1 : 0) + (suspiciousPitch ? 1 : 0);
        if (suspiciousAxes == 0) return null;

        final SnapBackSample sample = new SnapBackSample(pendingContext,
                                                         suspiciousAxes,
                                                         yawSnap,
                                                         pitchSnap,
                                                         yawReturn,
                                                         pitchReturn,
                                                         yawReturnError,
                                                         pitchReturnError,
                                                         Math.max(0L, currentTimestamp - pendingInteractionTimestamp),
                                                         pendingFollowingPackets);
        clearPendingInteraction();
        return sample;
    }

    private boolean pendingPathIsTooIndirect()
    {
        final double yawSnap = Math.abs(TargetingAnalysis.signedYawDelta(pendingInteractionYaw, pendingBeforeYaw));
        final double pitchSnap = Math.abs(pendingInteractionPitch - pendingBeforePitch);
        final boolean yawIndirect = yawSnap >= MINIMUM_YAW_SNAP && pendingYawPathLength > yawSnap / MINIMUM_RETURN_PATH_EFFICIENCY;
        final boolean pitchIndirect = pitchSnap >= MINIMUM_PITCH_SNAP && pendingPitchPathLength > pitchSnap / MINIMUM_RETURN_PATH_EFFICIENCY;
        return (yawIndirect && pitchIndirect) ||
               (yawIndirect && pitchSnap < MINIMUM_PITCH_SNAP) ||
               (pitchIndirect && yawSnap < MINIMUM_YAW_SNAP);
    }

    private static boolean isSnapBackAxis(final double snap,
                                          final double returned,
                                          final double returnError,
                                          final double pathLength,
                                          final double minimumSnap)
    {
        final double absoluteSnap = Math.abs(snap);
        if (absoluteSnap < minimumSnap || snap * returned >= 0D) return false;

        final double absoluteReturn = Math.abs(returned);
        final double totalOpposingMovement = absoluteSnap + absoluteReturn;
        final double cancellation = totalOpposingMovement == 0D
                                    ? 0D
                                    : 1D - returnError / totalOpposingMovement;
        final double pathEfficiency = pathLength == 0D ? 0D : absoluteReturn / pathLength;
        final double returnRatio = absoluteReturn / absoluteSnap;
        return returnRatio >= MINIMUM_RETURN_RATIO &&
               cancellation >= MINIMUM_RETURN_CANCELLATION &&
               pathEfficiency >= MINIMUM_RETURN_PATH_EFFICIENCY;
    }

    private int physicalIndex(final int logicalIndex)
    {
        final int oldestIndex = (writeIndex - size + BUFFER_CAPACITY) % BUFFER_CAPACITY;
        return (oldestIndex + logicalIndex) % BUFFER_CAPACITY;
    }

    private void clearPendingInteraction()
    {
        pendingInteraction = false;
        pendingContext = null;
        pendingBeforeYaw = 0D;
        pendingBeforePitch = 0D;
        pendingInteractionYaw = 0D;
        pendingInteractionPitch = 0D;
        pendingLastYaw = 0D;
        pendingLastPitch = 0D;
        pendingYawPathLength = 0D;
        pendingPitchPathLength = 0D;
        pendingInteractionTimestamp = 0L;
        pendingFollowingPackets = 0;
    }

    /**
     * Result of adding one movement-packet rotation sample.
     */
    public record RotationUpdate(boolean accepted, SnapBackSample snapBackSample) {
    }

    /**
     * Immutable packet-ordered snapshot used by the shared statistical analysis.
     */
    public record Snapshot(double[] yaw,
                           double[] pitch,
                           boolean[] trustedBreakBefore,
                           long firstSequence,
                           long lastSequence) {
        public Snapshot(final double[] yaw,
                        final double[] pitch,
                        final long firstSequence,
                        final long lastSequence)
        {
            this(yaw, pitch, new boolean[yaw.length], firstSequence, lastSequence);
        }

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
     * Immutable recent rotation history ending at an attack or scaffold placement.
     */
    public record InteractionSnapshot(TargetingContext context,
                                      double[] yaw,
                                      double[] pitch,
                                      long[] timestamp,
                                      long[] sequence,
                                      boolean[] trustedBreakBefore,
                                      long interactionTimestamp) {
        public InteractionSnapshot(final TargetingContext context,
                                   final double[] yaw,
                                   final double[] pitch,
                                   final long[] timestamp,
                                   final long[] sequence,
                                   final long interactionTimestamp)
        {
            this(context,
                 yaw,
                 pitch,
                 timestamp,
                 sequence,
                 new boolean[yaw.length],
                 interactionTimestamp);
        }

        public InteractionSnapshot
        {
            yaw = Arrays.copyOf(yaw, yaw.length);
            pitch = Arrays.copyOf(pitch, pitch.length);
            timestamp = Arrays.copyOf(timestamp, timestamp.length);
            sequence = Arrays.copyOf(sequence, sequence.length);
            trustedBreakBefore = Arrays.copyOf(trustedBreakBefore, trustedBreakBefore.length);
            if (yaw.length != pitch.length ||
                yaw.length != timestamp.length ||
                yaw.length != sequence.length ||
                yaw.length != trustedBreakBefore.length) {
                throw new IllegalArgumentException("interaction snapshot arrays must have the same length");
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
        public long[] timestamp()
        {
            return Arrays.copyOf(timestamp, timestamp.length);
        }

        @Override
        public long[] sequence()
        {
            return Arrays.copyOf(sequence, sequence.length);
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
    public record AcquisitionSnapshot(double[] x,
                                      double[] y,
                                      double[] z,
                                      double[] yaw,
                                      double[] pitch,
                                      long[] sequence) {
        public AcquisitionSnapshot
        {
            x = Arrays.copyOf(x, x.length);
            y = Arrays.copyOf(y, y.length);
            z = Arrays.copyOf(z, z.length);
            yaw = Arrays.copyOf(yaw, yaw.length);
            pitch = Arrays.copyOf(pitch, pitch.length);
            sequence = Arrays.copyOf(sequence, sequence.length);
            if (x.length != y.length ||
                x.length != z.length ||
                x.length != yaw.length ||
                x.length != pitch.length ||
                x.length != sequence.length) {
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

    /**
     * Metrics for a completed interaction rotation which returned to its pre-interaction angle.
     */
    public record SnapBackSample(TargetingContext context,
                                 int suspiciousAxes,
                                 double yawSnap,
                                 double pitchSnap,
                                 double yawReturn,
                                 double pitchReturn,
                                 double yawReturnError,
                                 double pitchReturnError,
                                 long delayNanos,
                                 int followingPackets) {
    }
}

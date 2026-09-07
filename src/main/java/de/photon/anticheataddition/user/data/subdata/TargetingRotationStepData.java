package de.photon.anticheataddition.user.data.subdata;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Learns a noisy common divisor from yaw deltas and confirms action-associated departures only after
 * rotation returns to the learned step, including while actions continue. No timing or sensitivity estimate alone is evidence of cheating.
 * All entry points are synchronized because lifecycle events and packets can arrive on different threads.
 */
public final class TargetingRotationStepData
{
    private static final int TRAINING_SIZE = 64;
    private static final int RECOVERY_SAMPLES = 12;
    private static final int REQUIRED_EPISODES = 6;
    private static final long GAP_NANOS = TimeUnit.SECONDS.toNanos(1);
    private static final long EVIDENCE_NANOS = TimeUnit.SECONDS.toNanos(90);
    private static final long REPORT_NANOS = TimeUnit.SECONDS.toNanos(15);
    private final double[] deltas = new double[TRAINING_SIZE];
    private final double[] errors = new double[TRAINING_SIZE];
    private final Map<Integer, Long> legacyHorses = new HashMap<>();
    private int vehicle = -1;
    private int size;
    private double step;
    private float lastYaw;
    private boolean hasYaw;
    private long lastMovement;
    private long suppressedUntil;
    private long lastEpisode;
    private long lastReport;
    private Sample pending;
    private int actionGrace;
    private boolean departing;
    private boolean departureAction;
    private int recovered;
    private int episodes;

    /** Marks the preceding movement and the next two movements, independent of network arrival intervals. */
    public synchronized void action()
    {
        if (pending != null) pending = new Sample(pending.delta(), pending.error(), true);
        actionGrace = 2;
    }

    /**
     * A movement without rotation still closes the preceding packet interval but does not invent a zero-angle sample.
     * Returns a confirmed report, or null. The caller decides whether to log or add VL.
     */
    public synchronized Report movement(final float yaw, final boolean hasRotation, final long now)
    {
        if (now < suppressedUntil) return null;
        if (lastMovement != 0 && (now < lastMovement || now - lastMovement > GAP_NANOS)) reset();
        lastMovement = now;
        if (lastEpisode != 0 && now - lastEpisode > EVIDENCE_NANOS) episodes = 0;

        if (hasRotation && !Float.isFinite(yaw)) {
            reset();
            return null;
        }

        final Report report = process(pending, now);
        pending = null;
        if (hasRotation) {
            if (hasYaw) {
                // Do not wrap yaw or clear the model on a large jump; skip that uncertain delta only.
                final double delta = Math.abs((double) yaw - lastYaw);
                final double error = Math.max(0.00001D, 8D * (Math.ulp(yaw) + Math.ulp(lastYaw)));
                if (delta <= 180D && delta >= 0.03D) {
                    pending = new Sample(delta, error, actionGrace > 0);
                }
            }
            lastYaw = yaw;
            hasYaw = true;
        }
        if (actionGrace > 0) actionGrace--;
        return report;
    }

    private Report process(final Sample sample, final long now)
    {
        if (sample == null) return null;
        if (step == 0) {
            train(sample);
            return null;
        }
        // At coarse float precision the inferred grid cannot be distinguished reliably.
        if (sample.error() > step * 0.02D) {
            clearModel();
            return null;
        }
        final boolean fits = fits(sample.delta(), sample.error(), step);
        final double refinement = fits ? 0 : refinement(sample);
        if (refinement > 0) {
            // An aliased estimate is not evidence. Narrow it in place: relearning from even counts
            // would otherwise let a client alternate the same refinement and reset forever.
            clearModel();
            step = refinement;
        } else if (!fits) {
            if (!departing) size = 0;
            departing = true;
            departureAction |= sample.action();
            recovered = 0;
            // Require a full window before abandoning the old model. One extra off-grid packet must
            // not erase it. Sustained new input (including a changed sensitivity) is learned safely.
            final double previous = step;
            if (train(sample)) {
                final double replacement = step;
                clearModel();
                step = replacement == previous ? 0 : replacement;
            }
        } else if (departing && ++recovered >= RECOVERY_SAMPLES) {
            final boolean associated = departureAction;
            departing = departureAction = false;
            recovered = size = 0;
            if (!associated) return null;
            lastEpisode = now;
            if (++episodes >= REQUIRED_EPISODES) {
                episodes = 0;
                if (lastReport == 0 || now - lastReport >= REPORT_NANOS) {
                    lastReport = now;
                    return new Report(step, REQUIRED_EPISODES);
                }
            }
        }
        return null;
    }

    private boolean train(final Sample sample)
    {
        deltas[size] = sample.delta();
        errors[size++] = sample.error();
        if (size < TRAINING_SIZE) return false;
        step = commonDivisor();
        // Retry only once per 32 new samples, keeping computation and memory bounded.
        System.arraycopy(deltas, TRAINING_SIZE / 2, deltas, 0, TRAINING_SIZE / 2);
        System.arraycopy(errors, TRAINING_SIZE / 2, errors, 0, TRAINING_SIZE / 2);
        size = TRAINING_SIZE / 2;
        return true;
    }

    /**
     * Approximate GCD fitting: enumerate divisors of the smallest delta, refine using all integer multiples,
     * and require the entire window to agree within float uncertainty. Exact Euclid on rounded floats is unstable.
     * The minimum step and search bound deliberately sacrifice coverage rather than invent a tiny fitting grid.
     */
    private double commonDivisor()
    {
        double smallest = Double.POSITIVE_INFINITY;
        for (final double delta : deltas) smallest = Math.min(smallest, delta);
        for (int divisor = 1; divisor <= 128; ++divisor) {
            final double candidate = smallest / divisor;
            if (candidate > 0.65D) continue;
            if (candidate < 0.009D) break;
            double numerator = 0;
            double denominator = 0;
            for (final double delta : deltas) {
                final double multiple = Math.rint(delta / candidate);
                numerator += multiple * delta;
                denominator += multiple * multiple;
            }
            final double refined = numerator / denominator;
            if (refined < 0.009D || refined > 0.65D) continue;
            boolean valid = true;
            int distinct = 0;
            for (int i = 0; i < TRAINING_SIZE; ++i) {
                if (errors[i] > refined * 0.02D || !fits(deltas[i], errors[i], refined)) {
                    valid = false;
                    break;
                }
                boolean seen = false;
                final long multiple = Math.round(deltas[i] / refined);
                for (int j = 0; j < i; ++j) {
                    if (Math.round(deltas[j] / refined) == multiple) {
                        seen = true;
                        break;
                    }
                }
                if (!seen) distinct++;
            }
            if (valid && distinct >= 8) return refined;
        }
        return 0;
    }

    private static boolean fits(final double delta, final double error, final double divisor)
    {
        return Math.abs(delta - Math.rint(delta / divisor) * divisor) <= error;
    }

    private double refinement(final Sample sample)
    {
        // A window containing only even mouse counts can overestimate the actual GCD. An odd count later must
        // refine the estimate, even if it happens during an attack. Also accepts commensurate sensitivity changes.
        for (int divisor = 2; divisor <= 128 && step / divisor >= 0.009D; ++divisor) {
            if (fits(sample.delta(), sample.error(), step / divisor)) return step / divisor;
        }
        return 0;
    }

    private void clearModel()
    {
        step = 0;
        size = episodes = recovered = 0;
        departing = departureAction = false;
        lastEpisode = 0;
    }

    public synchronized void reset()
    {
        clearModel();
        pending = null;
        hasYaw = false;
        actionGrace = 0;
        lastMovement = 0;
        // Keep report cooldown and suppression across ordinary resets.
    }

    public synchronized void suppress(final long now)
    {
        reset();
        suppressedUntil = now + TimeUnit.SECONDS.toNanos(3);
    }

    public synchronized void trackLegacyHorse(final int entityId, final boolean horse)
    {
        legacyHorses.values().removeIf(expiry -> expiry < System.nanoTime());
        if (horse) legacyHorses.put(entityId, Long.MAX_VALUE);
        else legacyHorses.remove(entityId);
    }

    public synchronized boolean isLegacyHorse(final int entityId)
    {
        return legacyHorses.getOrDefault(entityId, Long.MIN_VALUE) >= System.nanoTime();
    }

    public synchronized void removeEntities(final int[] ids)
    {
        // Preserve in-flight interactions briefly; expiry never extends from client packets.
        final long expiry = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        for (final int id : ids) legacyHorses.computeIfPresent(id, (key, previous) -> Math.min(previous, expiry));
        legacyHorses.values().removeIf(deadline -> deadline < System.nanoTime());
    }

    public synchronized boolean passengers(final int entityId, final int[] passengers, final int playerId)
    {
        for (final int passenger : passengers) {
            if (passenger == playerId) {
                vehicle = entityId;
                return true;
            }
        }
        if (vehicle != entityId) return false;
        vehicle = -1;
        return true;
    }

    public synchronized void clearEntities()
    {
        legacyHorses.clear();
        vehicle = -1;
    }

    public synchronized double getStep()
    {
        return step;
    }

    private record Sample(double delta, double error, boolean action) {}

    public record Report(double step, int episodes) {}
}

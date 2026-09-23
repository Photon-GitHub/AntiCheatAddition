package de.photon.anticheataddition.user;

import de.photon.anticheataddition.user.data.subdata.TargetingRotationStepData;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

final class TargetingRotationStepDataTest
{
    @Test
    void learnsOrdinaryFloatRoundedMouseCountsAndNeverFlagsThem()
    {
        for (final double step : new double[]{0.0096D, 0.0375D, 0.15D, 0.6144D}) {
            final Trace trace = new Trace();
            trace.normal(step, 160);
            assertEquals(step, trace.state.getStep(), 0.00001D);
            for (int i = 0; i < 1000; ++i) {
                if (i % 17 == 0) trace.state.action();
                trace.normal(step, 1);
            }
            assertEquals(0, trace.reports);
        }
    }

    @Test
    void detectsSixSeparateDeparturesOnlyAfterOrdinaryLookingReturns()
    {
        final Trace trace = new Trace();
        trace.normal(0.15D, 160);
        for (int i = 0; i < 5; ++i) trace.episode();
        assertEquals(0, trace.reports);
        trace.state.action();
        trace.move(0.173D);
        assertEquals(0, trace.reports);
        trace.normal(0.15D, 18);
        assertEquals(1, trace.reports);
    }

    @Test
    void sensitivityChangesDuringActionsRelearnWithoutReports()
    {
        final Trace trace = new Trace();
        trace.normal(0.15D, 160);
        for (final double step : new double[]{0.173D, 0.075D, 0.3D, 0.0096D, 0.6144D, 0.15D}) {
            trace.state.action();
            trace.normal(step, 160);
            assertEquals(0, trace.reports);
            final double learned = trace.state.getStep();
            assertTrue(learned > 0);
            // A multiple of the previous step remains compatible; estimating the slider is not the goal.
            assertEquals(step, Math.rint(step / learned) * learned, 0.00001D);
        }
    }

    @Test
    void aSensitivityChangeDiscardsEarlierUnreportedEpisodes()
    {
        final Trace trace = new Trace();
        trace.normal(0.15D, 160);
        for (int i = 0; i < 5; ++i) trace.episode();
        trace.normal(0.173D, 160);
        trace.normal(0.15D, 160);
        trace.episode();
        assertEquals(0, trace.reports);
    }

    @Test
    void aliasedGcdAndDivisorSensitivityChangesAreNotDepartures()
    {
        final Trace trace = new Trace();
        trace.normal(0.3D, 160); // Could be 0.15 sensitivity with exclusively even counts.
        for (int i = 0; i < 12; ++i) {
            trace.state.action();
            trace.move(0.15D);
            trace.normal(0.3D, 160);
            assertEquals(0.15D, trace.state.getStep(), 0.00001D);
        }
        assertEquals(0, trace.reports);
    }

    @Test
    void unassociatedDeparturesAndContinuousSpoofingAreNotConfirmedReturns()
    {
        final Trace trace = new Trace();
        trace.normal(0.15D, 160);
        for (int i = 0; i < 12; ++i) {
            trace.move(0.173D);
            trace.normal(0.15D, 160);
        }
        for (int i = 0; i < 300; ++i) {
            trace.state.action();
            trace.move(0.173D);
        }
        assertEquals(0, trace.reports);
    }

    @Test
    void fractionalInputAndSmoothCameraDoNotForceABaseline()
    {
        final Trace trace = new Trace();
        final Random random = new Random(17);
        for (int i = 0; i < 2000; ++i) {
            if (i % 13 == 0) trace.state.action();
            trace.move((random.nextDouble() - 0.5D) * 2D);
        }
        assertEquals(0D, trace.state.getStep());
        assertEquals(0, trace.reports);
    }

    @Test
    void inactivitySuppressionAndNonFiniteRotationsClearEvidence()
    {
        final Trace trace = new Trace();
        trace.normal(0.15D, 160);
        trace.episode();
        trace.now += TimeUnit.SECONDS.toNanos(2);
        trace.move(0.15D);
        assertEquals(0D, trace.state.getStep());
        trace.normal(0.15D, 160);
        trace.state.suppress(trace.now);
        trace.normal(0.15D, 40);
        assertEquals(0D, trace.state.getStep());
        trace.normal(0.15D, 160);
        assertNull(trace.state.movement(Float.NaN, true, trace.now));
        assertEquals(0D, trace.state.getStep());
        assertEquals(0, trace.reports);
    }

    @Test
    void movementWithoutRotationCannotManufactureRecoveryOrEraseTheModel()
    {
        final Trace trace = new Trace();
        trace.normal(0.15D, 160);
        for (int i = 0; i < 5; ++i) trace.episode();
        trace.state.action();
        trace.move(0.173D);
        for (int i = 0; i < 50; ++i) {
            trace.now += TimeUnit.MILLISECONDS.toNanos(50);
            assertNull(trace.state.movement(0, false, trace.now));
        }
        assertEquals(0, trace.reports);
        assertTrue(trace.state.getStep() > 0);
    }

    @Test
    void packetBurstsPreserveActionAssociationAndReportsAreRateLimited()
    {
        final Trace trace = new Trace();
        trace.interval = 1;
        trace.normal(0.15D, 160);
        for (int i = 0; i < 30; ++i) trace.episode();
        assertEquals(1, trace.reports);
    }

    @Test
    void coarseYawPrecisionAndLargeCameraChangesAbstain()
    {
        final Trace trace = new Trace();
        trace.yaw = 1_000_000F;
        trace.normal(0.15D, 160);
        assertEquals(0D, trace.state.getStep());
        trace.yaw = 0;
        trace.normal(0.15D, 160);
        trace.move(360D);
        assertTrue(trace.state.getStep() > 0);
        assertEquals(0, trace.reports);
    }

    @Test
    void actionSpamCannotPreventLearningOrRecovery()
    {
        final Trace trace = new Trace();
        for (int i = 0; i < 160; ++i) {
            trace.state.action();
            trace.normal(0.15D, 1);
        }
        assertEquals(0.15D, trace.state.getStep(), 0.00001D);
        for (int episode = 0; episode < 6; ++episode) {
            trace.state.action();
            trace.move(0.173D);
            for (int i = 0; i < 18; ++i) {
                trace.state.action();
                trace.normal(0.15D, 1);
            }
        }
        assertEquals(1, trace.reports);
    }

    @Test
    void extraUnassociatedMismatchAndMoreThanEightBadSamplesCannotEraseAnEpisode()
    {
        final Trace trace = new Trace();
        trace.normal(0.15D, 160);
        for (int episode = 0; episode < 6; ++episode) {
            trace.state.action();
            for (int i = 0; i < 10; ++i) trace.move(0.173D);
            trace.normal(0.15D, 18);
        }
        assertEquals(1, trace.reports);
    }

    @Test
    void continuousCombatAllowsRealSensitivityChanges()
    {
        final Trace trace = new Trace();
        for (final double step : new double[]{0.15D, 0.173D, 0.075D, 0.6144D, 0.15D}) {
            for (int i = 0; i < 200; ++i) {
                trace.state.action();
                trace.normal(step, 1);
            }
            assertTrue(trace.state.getStep() > 0);
            assertEquals(0, trace.reports);
        }
    }

    @Test
    void onlyOurPassengerTransitionsInvalidateCameraContinuity()
    {
        final var state = new TargetingRotationStepData();
        assertFalse(state.passengers(10, new int[]{2}, 1));
        assertTrue(state.passengers(10, new int[]{1}, 1));
        assertFalse(state.passengers(11, new int[]{2}, 1));
        assertTrue(state.passengers(10, new int[0], 1));
        assertFalse(state.passengers(10, new int[0], 1));
    }

    private static final class Trace
    {
        private final TargetingRotationStepData state = new TargetingRotationStepData();
        private float yaw;
        private long now = TimeUnit.SECONDS.toNanos(1);
        private long interval = TimeUnit.MILLISECONDS.toNanos(50);
        private int index;
        private int reports;

        private void move(final double delta)
        {
            yaw = (float) (yaw + delta);
            now += interval;
            if (state.movement(yaw, true, now) != null) reports++;
        }

        private void normal(final double step, final int count)
        {
            for (int i = 0; i < count; ++i) {
                final int n = index++;
                move(step * (1 + n % 13) * (n % 2 == 0 ? 1 : -1));
            }
        }

        private void episode()
        {
            state.action();
            move(0.173D);
            normal(0.15D, 18);
        }
    }
}

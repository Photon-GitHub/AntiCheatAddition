package de.photon.anticheataddition.modules;

import de.photon.anticheataddition.modules.checks.targeting.TargetingMixedAnalysis;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public final class TargetingMixedAnalysisTest
{
    @Test
    public void detectsRepeatedCrossWindowModeSwitching()
    {
        final int noise = TargetingMixedAnalysis.modeMask(true, false, false, false, false);
        final int pattern = TargetingMixedAnalysis.modeMask(false, false, true, false, false);
        final TargetingMixedAnalysis.Result result = TargetingMixedAnalysis.analyze(
                new int[]{noise, pattern, noise, pattern, noise});

        assertTrue(result.suspicious());
        assertEquals(5, result.suspiciousObservations());
        assertEquals(2, result.distinctModes());
        assertTrue(result.transitions() >= 2);
    }

    @Test
    public void ignoresRepeatedUseOfOneSuspiciousMode()
    {
        final int noise = TargetingMixedAnalysis.modeMask(true, false, false, false, false);
        final TargetingMixedAnalysis.Result result = TargetingMixedAnalysis.analyze(
                new int[]{noise, noise, noise, noise, noise, noise, noise});

        assertFalse(result.suspicious());
        assertEquals(1, result.distinctModes());
    }

    @Test
    public void ignoresSparseIsolatedClassifications()
    {
        final int noise = TargetingMixedAnalysis.modeMask(true, false, false, false, false);
        final int pattern = TargetingMixedAnalysis.modeMask(false, false, true, false, false);
        final TargetingMixedAnalysis.Result result = TargetingMixedAnalysis.analyze(
                new int[]{noise, 0, 0, 0, pattern, 0, 0, noise, 0, 0, 0, 0});

        assertFalse(result.suspicious());
        assertEquals(3, result.suspiciousObservations());
    }

    @Test
    public void naturalWindowsDoNotEraseModeTransitions()
    {
        final int noise = TargetingMixedAnalysis.modeMask(true, false, false, false, false);
        final int pattern = TargetingMixedAnalysis.modeMask(false, false, true, false, false);
        final TargetingMixedAnalysis.Result result = TargetingMixedAnalysis.analyze(
                new int[]{noise, 0, pattern, 0, noise, 0, pattern, 0, noise});

        assertTrue(result.suspicious());
    }
}

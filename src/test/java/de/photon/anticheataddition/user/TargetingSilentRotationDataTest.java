package de.photon.anticheataddition.user;

import de.photon.anticheataddition.user.data.subdata.TargetingSilentRotationData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class TargetingSilentRotationDataTest
{
    @Test
    public void permitsTheFirstFlagBeforeTheCooldownHasElapsedSinceJvmStart()
    {
        final TargetingSilentRotationData data = new TargetingSilentRotationData();
        for (int i = 0; i < 7; i++) data.observe(true, 8, 8, i + 1L, 1_000_000_000L);

        assertTrue(data.observe(true, 8, 8, 8L, 1_000_000_000L).shouldFlag());
    }

    @Test
    public void requiresTheConfiguredMismatchWindow()
    {
        final TargetingSilentRotationData data = new TargetingSilentRotationData();
        for (int i = 0; i < 11; i++) {
            assertFalse(data.observe(true, 12, 8, 2_000_000_000L + i, 1_000_000_000L).suspicious());
        }
        final var result = data.observe(true, 12, 8, 2_000_000_011L, 1_000_000_000L);
        assertTrue(result.suspicious());
        assertTrue(result.shouldFlag());
    }

    @Test
    public void slidesAndSuppressesImmediateDuplicateFlags()
    {
        final TargetingSilentRotationData data = new TargetingSilentRotationData();
        for (int i = 0; i < 8; i++) data.observe(true, 8, 8, 2_000_000_000L + i, 1_000_000_000L);
        assertFalse(data.observe(true, 8, 8, 2_000_000_001L, 1_000_000_000L).shouldFlag());
        assertTrue(data.observe(true, 8, 8, 3_000_000_008L, 1_000_000_000L).shouldFlag());

        final var reset = data.observe(false, 8, 8, 4_000_000_001L, 1_000_000_000L);
        assertFalse(reset.suspicious());
    }
}

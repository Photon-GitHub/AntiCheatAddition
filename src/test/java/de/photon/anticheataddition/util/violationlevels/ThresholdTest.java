package de.photon.anticheataddition.util.violationlevels;

import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.util.violationlevels.threshold.ThresholdManagement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ThresholdTest
{
    @BeforeAll
    static void setup()
    {
        Dummy.mockAntiCheatAddition("src/test/resources/thresholdTest.yml");
    }

    @Test
    void commandLoadingTest()
    {
        Assertions.assertThrows(NullPointerException.class, () -> ThresholdManagement.loadCommands(null), "Can load null path.");
        Assertions.assertThrows(IllegalArgumentException.class, () -> ThresholdManagement.loadCommands("THIS_KEY_DOES_NOT_EXIST_REALLY!"), "Nonexistent key loaded.");

        Assertions.assertDoesNotThrow(() -> ThresholdManagement.loadCommands("NoCommand"));
        Assertions.assertDoesNotThrow(() -> ThresholdManagement.loadCommands("SingleCommand"));
        Assertions.assertDoesNotThrow(() -> ThresholdManagement.loadCommands("MultiCommand"));
    }

    @Test
    void thresholdLoadingTest()
    {
        Assertions.assertThrows(NullPointerException.class, () -> ThresholdManagement.loadThresholds((String) null), "Can load null path.");
        Assertions.assertThrows(NullPointerException.class, () -> ThresholdManagement.loadThresholds("THIS_KEY_DOES_NOT_EXIST_REALLY!"), "Nonexistent key loaded.");
        Assertions.assertThrows(NullPointerException.class, () -> ThresholdManagement.loadThresholds("NoCommandThreshold"), "Loaded malformed threshold.");

        Assertions.assertDoesNotThrow(() -> ThresholdManagement.loadThresholds("SingleCommandThreshold"));
        Assertions.assertDoesNotThrow(() -> ThresholdManagement.loadThresholds("SingleCommandThresholdTwo"));
        Assertions.assertDoesNotThrow(() -> ThresholdManagement.loadThresholds("MultiCommandThreshold"));
    }
}

package de.photon.anticheataddition.util.violationlevels;

import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.util.violationlevels.threshold.Threshold;
import de.photon.anticheataddition.util.violationlevels.threshold.ThresholdManagement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

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
        Assertions.assertThrows(NullPointerException.class, () -> ThresholdManagement.loadThresholds("MalformedCommandThreshold"), "Loaded malformed threshold.");

        Assertions.assertDoesNotThrow(() -> ThresholdManagement.loadThresholds("SingleCommandThreshold"));
        Assertions.assertDoesNotThrow(() -> ThresholdManagement.loadThresholds("SingleCommandThresholdTwo"));
        Assertions.assertDoesNotThrow(() -> ThresholdManagement.loadThresholds("MultiCommandThreshold"));
    }

    @Test
    void thresholdCorrectLoadingTest()
    {
        final List<Threshold> noThreshold = ThresholdManagement.loadThresholds("NoCommandThreshold").getThresholds();
        Assertions.assertEquals(0, noThreshold.size());

        final List<Threshold> singleThreshold = ThresholdManagement.loadThresholds("SingleCommandThreshold").getThresholds();
        Assertions.assertEquals(1, singleThreshold.size());
        Assertions.assertEquals(1, singleThreshold.getFirst().commandList().size());
        Assertions.assertLinesMatch(List.of("Some command"), singleThreshold.getFirst().commandList());

        final List<Threshold> multiThreshold = ThresholdManagement.loadThresholds("MultiCommandThreshold").getThresholds();
        Assertions.assertEquals(3, multiThreshold.size());

        Assertions.assertEquals(1, multiThreshold.get(0).commandList().size());
        Assertions.assertLinesMatch(List.of("Some command"), multiThreshold.get(0).commandList());

        Assertions.assertEquals(2, multiThreshold.get(1).commandList().size());
        Assertions.assertLinesMatch(List.of("Another command", "Yes, this as well."), multiThreshold.get(1).commandList());

        Assertions.assertEquals(1, multiThreshold.get(2).commandList().size());
        Assertions.assertLinesMatch(List.of("Yup."), multiThreshold.get(2).commandList());
    }
}

package de.photon.anticheataddition.util.violationlevels;

import de.photon.anticheataddition.Dummy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ViolationLevelManagementTest
{
    @BeforeAll
    static void setup()
    {
        Dummy.mockAntiCheatAddition();
    }

    @Test
    void builderTest()
    {
        Assertions.assertThrows(Exception.class, () -> ViolationLevelManagement.builder(Dummy.mockViolationModule("Inventory")).build(), "Can create ViolationLevelManagement without ThresholdManagement.");
        Assertions.assertDoesNotThrow(() -> ViolationLevelManagement.builder(Dummy.mockViolationModule("Inventory")).emptyThresholdManagement().build(), "Cannot create ViolationLevelManagement with empty ThresholdManagement.");
    }

    @Test
    void vlTest()
    {
        final var vlm = ViolationLevelManagement.builder(Dummy.mockViolationModule("Inventory")).emptyThresholdManagement().build();
        final var players = Dummy.mockDistinctPlayers(2);
        vlm.setVL(players[0], 100);
        vlm.addVL(players[0], 10);
        Assertions.assertEquals(110, vlm.getVL(players[0].getUniqueId()));
        Assertions.assertEquals(0, vlm.getVL(players[1].getUniqueId()));
    }
}

package de.photon.anticheataddition.util.violationlevels;

import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.util.violationlevels.threshold.ThresholdManagement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

class ViolationAggregationTest
{
    @BeforeAll
    static void setup()
    {
        Dummy.mockAntiCheatAddition();
    }

    @Test
    void initTest()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ViolationAggregation(Dummy.mockViolationModule("Inventory"), ThresholdManagement.EMPTY, Set.of()), "Can create ViolationAggregation without children.");

        final var vlm = ViolationLevelManagement.builder(Dummy.mockViolationModule("Inventory")).emptyThresholdManagement().build();
        final var vlAggregation = new ViolationAggregation(Dummy.mockViolationModule("Inventory"), ThresholdManagement.EMPTY, Set.of(vlm));
        final var player = Dummy.mockPlayer();

        Assertions.assertThrows(UnsupportedOperationException.class, () -> vlAggregation.setVL(player, 0));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> vlAggregation.addVL(player, 1));
    }

    @Test
    void vlTest()
    {
        final var vlm1 = ViolationLevelManagement.builder(Dummy.mockViolationModule("Inventory.parts.Hit")).emptyThresholdManagement().build();
        final var vlm2 = ViolationLevelManagement.builder(Dummy.mockViolationModule("Inventory.parts.Rotation")).emptyThresholdManagement().build();

        final var vlAggregation = new ViolationAggregation(Dummy.mockViolationModule("Inventory"), ThresholdManagement.EMPTY, Set.of(vlm1, vlm2));

        final var players = Dummy.mockDistinctPlayers(2);
        vlm1.setVL(players[0], 100);
        vlm2.addVL(players[0], 10);

        Assertions.assertEquals(110, vlAggregation.getVL(players[0].getUniqueId()));
        Assertions.assertEquals(0, vlAggregation.getVL(players[1].getUniqueId()));

        vlm2.addVL(players[1], 20);
        Assertions.assertEquals(20, vlAggregation.getVL(players[1].getUniqueId()));

        vlm1.setVL(players[1], 120);
        Assertions.assertEquals(140, vlAggregation.getVL(players[1].getUniqueId()));
        vlm2.setVL(players[1], 0);
        Assertions.assertEquals(120, vlAggregation.getVL(players[1].getUniqueId()));
    }
}

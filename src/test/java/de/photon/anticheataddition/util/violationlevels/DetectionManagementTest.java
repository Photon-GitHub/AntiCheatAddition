package de.photon.anticheataddition.util.violationlevels;

import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.modules.ViolationModule;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

class DetectionManagementTest
{
    // Do not remove this unused variable, it is needed for initialization of mocking.
    private static final ViolationModule dummyVlModule = Dummy.mockViolationModule("Sentinel.LabyMod");

    private static DetectionManagement getDetectionManagement()
    {
        return new DetectionManagement(dummyVlModule)
        {
            // Disable punishment to avoid bugs.
            protected void punishPlayer(@NotNull Player player, int fromVl, int toVl) {}
        };
    }

    @BeforeAll
    static void setup()
    {
        Dummy.mockAntiCheatAddition();
    }

    @Test
    void invalidVl()
    {
        final var player = Dummy.mockPlayer();
        final var management = getDetectionManagement();

        Assertions.assertDoesNotThrow(() -> management.setVL(player, 0));
        Assertions.assertDoesNotThrow(() -> management.addVL(player, 0));
        Assertions.assertDoesNotThrow(() -> management.setVL(player, 1));
        Assertions.assertDoesNotThrow(() -> management.addVL(player, 0));

        Assertions.assertThrows(IllegalArgumentException.class, () -> management.setVL(player, -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> management.addVL(player, -1));

        IntStream.rangeClosed(2, 10).forEach(i -> {
            Assertions.assertThrows(IllegalArgumentException.class, () -> management.setVL(player, i));
            Assertions.assertThrows(IllegalArgumentException.class, () -> management.setVL(player, -i));
            Assertions.assertThrows(IllegalArgumentException.class, () -> management.addVL(player, i));
            Assertions.assertThrows(IllegalArgumentException.class, () -> management.addVL(player, -i));
        });

        ThreadLocalRandom.current().ints(10, 11, Integer.MAX_VALUE).forEach(i -> {
            Assertions.assertThrows(IllegalArgumentException.class, () -> management.setVL(player, i));
            Assertions.assertThrows(IllegalArgumentException.class, () -> management.setVL(player, -i));
            Assertions.assertThrows(IllegalArgumentException.class, () -> management.addVL(player, i));
            Assertions.assertThrows(IllegalArgumentException.class, () -> management.addVL(player, -i));
        });
    }

    @Test
    void testSetVl()
    {
        final var players = Dummy.mockDistinctPlayers(2);
        final var management = getDetectionManagement();

        management.setVL(players[0], 0);
        management.setVL(players[1], 0);
        Assertions.assertEquals(0, management.getVL(players[0].getUniqueId()));
        Assertions.assertEquals(0, management.getVL(players[1].getUniqueId()));
        management.setVL(players[0], 1);
        Assertions.assertEquals(1, management.getVL(players[0].getUniqueId()));
        Assertions.assertEquals(0, management.getVL(players[1].getUniqueId()));
        management.setVL(players[1], 1);
        Assertions.assertEquals(1, management.getVL(players[0].getUniqueId()));
        Assertions.assertEquals(1, management.getVL(players[1].getUniqueId()));
        management.setVL(players[0], 0);
        Assertions.assertEquals(0, management.getVL(players[0].getUniqueId()));
        Assertions.assertEquals(1, management.getVL(players[1].getUniqueId()));
        management.setVL(players[1], 0);
        Assertions.assertEquals(0, management.getVL(players[0].getUniqueId()));
        Assertions.assertEquals(0, management.getVL(players[1].getUniqueId()));
    }

    @Test
    void testAddVl()
    {
        final var management = getDetectionManagement();
        final var player = Dummy.mockPlayer();

        management.setVL(player, 0);
        Assertions.assertEquals(0, management.getVL(player.getUniqueId()));
        management.addVL(player, 0);
        Assertions.assertEquals(0, management.getVL(player.getUniqueId()));
        management.addVL(player, 1);
        Assertions.assertEquals(1, management.getVL(player.getUniqueId()));
        management.addVL(player, 1);
        Assertions.assertEquals(1, management.getVL(player.getUniqueId()));
        management.addVL(player, 0);
        Assertions.assertEquals(1, management.getVL(player.getUniqueId()));
    }


    @Test
    void testPunishPlayerCalledOnTransitionToOne() {
        final var player = Dummy.mockPlayer();
        final AtomicInteger punishCount = new AtomicInteger(0);

        // Create a DetectionManagement that counts punishPlayer calls
        var management = new DetectionManagement(dummyVlModule) {
            @Override
            protected void punishPlayer(@NotNull Player p, int fromVl, int toVl) {
                // Should only ever be called with fromVl=0, toVl=1
                Assertions.assertEquals(player, p, "Wrong player in punishPlayer");
                Assertions.assertEquals(0, fromVl, "Expected fromVl to be 0");
                Assertions.assertEquals(1, toVl, "Expected toVl to be 1");
                punishCount.incrementAndGet();
            }
        };

        // Initial set to 0: no punishment
        management.setVL(player, 0);
        Assertions.assertEquals(0, punishCount.get(), "No punish on setVL(0)");

        // First transition 0 -> 1: punish once
        management.setVL(player, 1);
        Assertions.assertEquals(1, punishCount.get(), "punishPlayer should have been called once");

        // Repeated set to 1: no additional punishment
        management.setVL(player, 1);
        Assertions.assertEquals(1, punishCount.get(), "punishPlayer should NOT be called again for idempotent setVL(1)");

        // Reset back to 0: no punishment
        management.setVL(player, 0);
        Assertions.assertEquals(1, punishCount.get(), "No punish on setVL(0) reset");

        // Another 0 -> 1 transition: punish again
        management.setVL(player, 1);
        Assertions.assertEquals(2, punishCount.get(), "punishPlayer should be called again on new transition 0->1");
    }
}

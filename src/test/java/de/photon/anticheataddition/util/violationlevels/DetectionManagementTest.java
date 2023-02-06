package de.photon.anticheataddition.util.violationlevels;

import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

class DetectionManagementTest
{
    // Do not remove this unused variable, it is needed for initialization of mocking.
    private static final ViolationModule dummyVlModule = Dummy.mockViolationModule("Sentinel.LabyMod");
    private static User dummy;

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
        dummy = Dummy.mockUser();
    }

    @Test
    void invalidVl()
    {
        final var player = dummy.getPlayer();
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
        final var player = dummy.getPlayer();
        final var player2 = Dummy.mockPlayer();
        final var management = getDetectionManagement();

        management.setVL(player, 0);
        management.setVL(player2, 0);
        Assertions.assertEquals(0, management.getVL(player.getUniqueId()));
        Assertions.assertEquals(0, management.getVL(player2.getUniqueId()));
        management.setVL(player, 1);
        Assertions.assertEquals(1, management.getVL(player.getUniqueId()));
        Assertions.assertEquals(0, management.getVL(player2.getUniqueId()));
        management.setVL(player2, 1);
        Assertions.assertEquals(1, management.getVL(player.getUniqueId()));
        Assertions.assertEquals(1, management.getVL(player2.getUniqueId()));
        management.setVL(player, 0);
        Assertions.assertEquals(0, management.getVL(player.getUniqueId()));
        Assertions.assertEquals(1, management.getVL(player2.getUniqueId()));
        management.setVL(player2, 0);
        Assertions.assertEquals(0, management.getVL(player.getUniqueId()));
        Assertions.assertEquals(0, management.getVL(player2.getUniqueId()));
    }

    @Test
    void testAddVl()
    {
        final var management = getDetectionManagement();
        final var player = dummy.getPlayer();

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
}

package de.photon.anticheataddition.util.violationlevels;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.modules.ViolationModule;
import de.photon.anticheataddition.user.User;
import lombok.val;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.stream.IntStream;

class DetectionManagementTest
{
    // Do not remove this unused variable, it is needed for initialization of mocking.
    private static final AntiCheatAddition mock = Dummy.mockAACAdditionPro();
    private static final User dummy = Dummy.mockUser();
    private static final ViolationModule dummyVlModule = Dummy.mockViolationModule("Sentinel.LabyMod");

    private static DetectionManagement getDetectionManagement()
    {
        return new DetectionManagement(dummyVlModule)
        {
            // Disable punishment to avoid bugs.
            protected void punishPlayer(@NotNull Player player, int fromVl, int toVl) {}
        };
    }

    @Test
    void invalidVl()
    {
        val player = dummy.getPlayer();
        val management = getDetectionManagement();

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

        new Random().ints(10, 11, Integer.MAX_VALUE).forEach(i -> {
            Assertions.assertThrows(IllegalArgumentException.class, () -> management.setVL(player, i));
            Assertions.assertThrows(IllegalArgumentException.class, () -> management.setVL(player, -i));
            Assertions.assertThrows(IllegalArgumentException.class, () -> management.addVL(player, i));
            Assertions.assertThrows(IllegalArgumentException.class, () -> management.addVL(player, -i));
        });
    }

    @Test
    void testSetVl()
    {
        val player = dummy.getPlayer();
        val player2 = Dummy.mockPlayer();
        val management = getDetectionManagement();

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
        val management = getDetectionManagement();
        val player = dummy.getPlayer();

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

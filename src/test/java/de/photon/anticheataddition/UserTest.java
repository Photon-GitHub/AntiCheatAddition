package de.photon.anticheataddition;

import de.photon.anticheataddition.user.User;
import lombok.val;
import org.bukkit.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UserTest
{
    private static User dummyUser;

    @BeforeAll
    static void mockACA()
    {
        Dummy.mockAntiCheatAddition();
        dummyUser = Dummy.mockUser();
    }

    @Test
    void materialTest()
    {
        val obsidian = Material.OBSIDIAN;

        dummyUser.getData().object.lastMaterialClicked = obsidian;
        Assertions.assertSame(dummyUser.getData().object.lastMaterialClicked, obsidian);
    }

    @Test
    void counterTest()
    {
        val counter = dummyUser.getData().counter.inventoryAverageHeuristicsMisclicks;

        counter.setToZero();
        Assertions.assertSame(0L, counter.getCounter());
        counter.increment();
        Assertions.assertSame(1L, counter.getCounter());
        counter.increment();
        Assertions.assertSame(2L, counter.getCounter());
        counter.decrement();
        Assertions.assertSame(1L, counter.getCounter());
        counter.decrementAboveZero();
        Assertions.assertSame(0L, counter.getCounter());
        counter.decrementAboveZero();
        Assertions.assertSame(0L, counter.getCounter());
    }
}

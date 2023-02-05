package de.photon.anticheataddition;

import de.photon.anticheataddition.user.User;
import org.bukkit.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UserTest
{
    private static User[] dummyUsers;

    @BeforeAll
    static void mockACA()
    {
        Dummy.mockAntiCheatAddition();
        dummyUsers = new User[]{Dummy.mockUser(), Dummy.mockUser()};
    }

    @Test
    void userDataMaterialTest()
    {
        final var obsidian = Material.OBSIDIAN;
        final var coalBlock = Material.COAL_BLOCK;

        dummyUsers[0].getData().object.lastMaterialClicked = obsidian;
        dummyUsers[1].getData().object.lastMaterialClicked = coalBlock;

        Assertions.assertSame(dummyUsers[0].getData().object.lastMaterialClicked, obsidian);
        Assertions.assertSame(dummyUsers[1].getData().object.lastMaterialClicked, coalBlock);
    }

    @Test
    void counterTest()
    {
        final var counter = dummyUsers[0].getData().counter.inventoryAverageHeuristicsMisclicks;

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

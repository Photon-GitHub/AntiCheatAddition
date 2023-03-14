package de.photon.anticheataddition.user;

import de.photon.anticheataddition.Dummy;
import org.bukkit.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UserTest
{
    @BeforeAll
    static void mockACA()
    {
        Dummy.mockAntiCheatAddition();
    }

    @Test
    void userDataMaterialTest()
    {
        final var dummyUsers = Dummy.mockDistinctUsers(2);

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
        final var dummyUser = Dummy.mockUser();
        final var counter = dummyUser.getData().counter.inventoryAverageHeuristicsMisclicks;

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

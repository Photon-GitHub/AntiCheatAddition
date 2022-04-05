package de.photon.anticheataddition;

import de.photon.anticheataddition.user.data.DataKey;
import lombok.val;
import org.bukkit.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class UserTest
{
    @BeforeAll
    static void mockACA()
    {
        Dummy.mockEnvironment();
        Dummy.mockAntiCheatAddition();
    }

    @Test
    void materialTest()
    {
        val user = Dummy.mockUser();
        val obsidian = Material.OBSIDIAN;

        user.getDataMap().setObject(DataKey.Obj.LAST_MATERIAL_CLICKED, obsidian);
        Assertions.assertSame(user.getDataMap().getObject(DataKey.Obj.LAST_MATERIAL_CLICKED), obsidian);
    }

    @Test
    void counterTest()
    {
        val user = Dummy.mockUser();
        val counter = user.getDataMap().getCounter(DataKey.Count.INVENTORY_AVERAGE_HEURISTICS_MISCLICKS);

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

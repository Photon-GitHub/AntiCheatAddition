package de.photon.aacadditionpro;

import de.photon.aacadditionpro.user.data.DataKey;
import lombok.val;
import org.bukkit.Material;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class UserTest
{
    @Test
    void materialTest()
    {
        Dummy.mockAACAdditionPro();

        val obsidian = Material.OBSIDIAN;
        val user = Dummy.mockUser();

        user.getDataMap().setObject(DataKey.ObjectKey.LAST_MATERIAL_CLICKED, obsidian);
        Assertions.assertSame(user.getDataMap().getObject(DataKey.ObjectKey.LAST_MATERIAL_CLICKED), obsidian);
    }

    @Test
    void counterTest()
    {
        Dummy.mockAACAdditionPro();
        val user = Dummy.mockUser();
        val counter = user.getDataMap().getCounter(DataKey.CounterKey.INVENTORY_AVERAGE_HEURISTICS_MISCLICKS);

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

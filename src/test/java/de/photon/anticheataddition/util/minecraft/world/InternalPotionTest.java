package de.photon.anticheataddition.util.minecraft.world;

import de.photon.anticheataddition.Dummy;
import de.photon.anticheataddition.util.minecraft.world.entity.InternalPotion;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class InternalPotionTest
{
    @BeforeAll
    static void setup()
    {
        Dummy.mockEnvironment();
    }

    @Test
    void potionTypeCoverageTest()
    {
        // This test only ensures that no PotionEffectType is forgotten in InternalPotion.
        Assertions.assertSame(InternalPotion.values().length, PotionEffectType.values().length);
    }
}

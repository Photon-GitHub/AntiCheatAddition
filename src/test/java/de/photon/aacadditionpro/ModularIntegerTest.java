package de.photon.aacadditionpro;

import de.photon.aacadditionpro.util.mathematics.ModularInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

public class ModularIntegerTest
{
    @Test
    public void IntegerTest()
    {
        final Random random = new Random();

        Assertions.assertEquals(0, ModularInteger.increment(Integer.MAX_VALUE - 1, Integer.MAX_VALUE));

        int x;
        for (int i = 0; i < 1000; ++i) {
            x = random.nextInt(Integer.MAX_VALUE);
            if (x == 0) continue;

            Assertions.assertEquals(0, ModularInteger.increment(x, x + 1));
            Assertions.assertEquals(0, ModularInteger.increment(x - 1, x));
            Assertions.assertEquals(x + 1, ModularInteger.increment(x, Integer.MAX_VALUE));
        }

        Assertions.assertEquals(Integer.MAX_VALUE - 1, ModularInteger.decrement(0, Integer.MAX_VALUE));

        for (int i = 0; i < 1000; ++i) {
            x = random.nextInt(Integer.MAX_VALUE);
            if (x == 0) continue;

            Assertions.assertEquals(x - 1, ModularInteger.decrement(x, Integer.MAX_VALUE));
            Assertions.assertEquals(x - 1, ModularInteger.decrement(0, x));
            Assertions.assertEquals(0, ModularInteger.decrement(1, x));
        }
    }
}

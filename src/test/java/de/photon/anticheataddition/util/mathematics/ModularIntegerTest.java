package de.photon.anticheataddition.util.mathematics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Random;

class ModularIntegerTest
{
    private final Random random = new Random();

    @Test
    void IntegerSetTest()
    {
        int x;
        int mod;
        for (int i = 0; i < 1000; ++i) {
            x = random.nextInt();
            // Shift to prevent integer overflow in the expected calculation below.
            mod = random.nextInt((Integer.MAX_VALUE >> 2) - 1) + 2;

            if (x > 1) {
                Assertions.assertEquals(0, ModularInteger.set(x, x));
                Assertions.assertEquals(x - 1, ModularInteger.set(x - 1, x));
            }

            Assertions.assertEquals(((x % mod) + mod) % mod, ModularInteger.set(x, mod), "x: " + x + " mod: " + mod);
        }
    }

    @Test
    void IntegerIncrementTest()
    {
        Assertions.assertEquals(0, ModularInteger.increment(Integer.MAX_VALUE - 1, Integer.MAX_VALUE));

        random.ints(1000, 2, Integer.MAX_VALUE).forEach(x -> {
            Assertions.assertEquals(0, ModularInteger.increment(x, x + 1));
            Assertions.assertEquals(0, ModularInteger.increment(x - 1, x));
            Assertions.assertEquals(x + 1, ModularInteger.increment(x, Integer.MAX_VALUE));
        });
    }

    @Test
    void IntegerDecrementTest()
    {
        Assertions.assertEquals(Integer.MAX_VALUE - 1, ModularInteger.decrement(0, Integer.MAX_VALUE));

        random.ints(1000, 2, Integer.MAX_VALUE).forEach(x -> {
            Assertions.assertEquals(x - 1, ModularInteger.decrement(x, Integer.MAX_VALUE));
            Assertions.assertEquals(x - 1, ModularInteger.decrement(0, x));
            Assertions.assertEquals(0, ModularInteger.decrement(1, x));
        });
    }

    @Test
    void modularIntegerBasicTest()
    {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ModularInteger(0, -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ModularInteger(0, 0));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new ModularInteger(0, 1));
        Assertions.assertDoesNotThrow(() -> new ModularInteger(0, 2));

        final ModularInteger modInt = new ModularInteger(0, 3);
        Assertions.assertEquals(0, modInt.getAndDecrement());
        Assertions.assertEquals(2, modInt.getAndDecrement());
        Assertions.assertEquals(1, modInt.get());

        Assertions.assertEquals(2, modInt.incrementAndGet());
        Assertions.assertEquals(2, modInt.getAndIncrement());
        Assertions.assertEquals(0, modInt.get());


        Assertions.assertEquals(2, modInt.set(2).get());
        Assertions.assertEquals(1, modInt.mul(2).get());
        Assertions.assertEquals(2, modInt.mul(2).get());


        Assertions.assertEquals(2, modInt.add(3).get());
        Assertions.assertEquals(1, modInt.add(2).get());
        Assertions.assertEquals(0, modInt.setToZero().get());


        Assertions.assertEquals(0, modInt.sub(30).get());
        Assertions.assertEquals(0, modInt.add(30).get());
        Assertions.assertEquals(1, modInt.sub(32).get());
    }
}

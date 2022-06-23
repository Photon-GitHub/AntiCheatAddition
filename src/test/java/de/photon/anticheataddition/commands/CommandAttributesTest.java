package de.photon.anticheataddition.commands;

import de.photon.anticheataddition.Dummy;
import lombok.val;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CommandAttributesTest
{
    @Test
    void testCommandHelp()
    {
        val commandAttributes = CommandAttributes.builder().addCommandHelp("This is a help message").build();
        Assertions.assertTrue(commandAttributes.hasCommandHelp());
    }

    @Test
    void testArgumentsOutOfRange()
    {
        val player = Dummy.mockPlayer();

        // Test all possible combinations of arguments. Default values are min = 0 and max = 100.
        for (int i = 0; i <= 100; ++i) {
            val exactAttributes = CommandAttributes.builder().exactArguments(i).build();
            val minAttributes = CommandAttributes.builder().minArguments(i).maxArguments(Integer.MAX_VALUE).build();
            val maxAttributes = CommandAttributes.builder().minArguments(Integer.MIN_VALUE).maxArguments(i).build();

            final int finalI = i;
            Assertions.assertFalse(exactAttributes.argumentsOutOfRange(finalI, player), () -> "Exact: " + finalI + " | Tested: " + (finalI + 1));
            Assertions.assertFalse(minAttributes.argumentsOutOfRange(finalI, player), () -> "Min: " + finalI + " | Tested: " + (finalI + 1));
            Assertions.assertFalse(maxAttributes.argumentsOutOfRange(finalI, player), () -> "Max: " + finalI + " | Tested: " + (finalI + 1));

            Assertions.assertTrue(exactAttributes.argumentsOutOfRange(finalI + 1, player), () -> "Exact: " + finalI + " | Tested: " + (finalI + 1));
            Assertions.assertFalse(minAttributes.argumentsOutOfRange(finalI + 1, player), () -> "Min: " + finalI + " | Tested: " + (finalI + 1));
            Assertions.assertTrue(maxAttributes.argumentsOutOfRange(finalI + 1, player), () -> "Max: " + finalI + " | Tested: " + (finalI + 1));


            Assertions.assertTrue(exactAttributes.argumentsOutOfRange(finalI - 1, player), () -> "Exact: " + finalI + " | Tested: " + (finalI - 1));
            Assertions.assertTrue(minAttributes.argumentsOutOfRange(finalI - 1, player), () -> "Min: " + finalI + " | Tested: " + (finalI - 1));
            Assertions.assertFalse(maxAttributes.argumentsOutOfRange(finalI - 1, player), () -> "Max: " + finalI + " | Tested: " + (finalI - 1));
        }
    }
}

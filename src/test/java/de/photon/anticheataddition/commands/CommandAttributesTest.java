package de.photon.anticheataddition.commands;

import de.photon.anticheataddition.Dummy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CommandAttributesTest
{
    @Test
    void testCommandHelp()
    {
        final var commandAttributes = CommandAttributes.builder().addCommandHelp("This is a help message").build();
        Assertions.assertTrue(commandAttributes.hasCommandHelp());
    }

    @Test
    void testArgumentsOutOfRange()
    {
        final var player = Dummy.mockPlayer();

        Assertions.assertThrows(IllegalArgumentException.class, () -> CommandAttributes.builder().minArguments(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommandAttributes.builder().maxArguments(-1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> CommandAttributes.builder().exactArguments(-1));

        Assertions.assertFalse(CommandAttributes.builder().exactArguments(0).build().argumentsOutOfRange(0, player), "0 was tested as the given exact arg, but was classified as out of range.");
        Assertions.assertFalse(CommandAttributes.builder().minArguments(0).build().argumentsOutOfRange(0, player), "0 was tested as the given min arg, but was classified as out of range.");
        Assertions.assertFalse(CommandAttributes.builder().maxArguments(0).build().argumentsOutOfRange(0, player), "0 was tested as the given max arg, but was classified as out of range.");

        // Test all possible combinations of arguments. Default values are min = 0 and max = 25.
        for (int i = 1; i <= 25; ++i) {
            final var exactAttributes = CommandAttributes.builder().exactArguments(i).build();
            final var minAttributes = CommandAttributes.builder().minArguments(i).maxArguments(Integer.MAX_VALUE).build();
            final var maxAttributes = CommandAttributes.builder().minArguments(0).maxArguments(i).build();

            final int finalI = i;
            Assertions.assertFalse(exactAttributes.argumentsOutOfRange(finalI, player), () -> finalI + " was tested as the given exact arg, but was classified as out of range.");
            Assertions.assertFalse(minAttributes.argumentsOutOfRange(finalI, player), () -> finalI + " was tested as the given min arg, but was classified as out of range.");
            Assertions.assertFalse(maxAttributes.argumentsOutOfRange(finalI, player), () -> finalI + " was tested as the given max arg, but was classified as out of range.");

            Assertions.assertTrue(exactAttributes.argumentsOutOfRange(finalI + 1, player), () -> "Tested " + finalI + ", which is exact + 1, but was not classified as out of range.");
            Assertions.assertFalse(minAttributes.argumentsOutOfRange(finalI + 1, player), () -> "Tested " + finalI + ", which is min + 1, but was classified as out of range.");
            Assertions.assertTrue(maxAttributes.argumentsOutOfRange(finalI + 1, player), () -> "Tested " + finalI + ", which is max + 1, but was not classified as out of range.");


            Assertions.assertTrue(exactAttributes.argumentsOutOfRange(finalI - 1, player), () -> "Tested " + finalI + ", which is exact - 1, but was not classified as out of range.");
            Assertions.assertTrue(minAttributes.argumentsOutOfRange(finalI - 1, player), () -> "Tested " + finalI + ", which is min - 1, but was not classified as out of range.");
            Assertions.assertFalse(maxAttributes.argumentsOutOfRange(finalI - 1, player), () -> "Tested " + finalI + ", which is max - 1, but was classified as out of range.");
        }
    }
}

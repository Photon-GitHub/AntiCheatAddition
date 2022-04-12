package de.photon.anticheataddition.util.config;

import de.photon.anticheataddition.AntiCheatAddition;
import de.photon.anticheataddition.Dummy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

class ConfigUtilTest
{
    @BeforeAll
    static void setup()
    {
        Dummy.mockAntiCheatAddition("src/test/resources/configUtilTest.yml");
    }

    @Test
    void testKeys()
    {
        Assertions.assertThrows(NullPointerException.class, () -> ConfigUtil.loadKeys("THIS_PATH_DOES_NOT_EXIST.REALLY"));

        var expected = new String[]{"Empty", "One", "Two", "Three"};
        Arrays.sort(expected);

        var actual = ConfigUtil.loadKeys("Test").stream().sorted().toArray(String[]::new);
        Assertions.assertArrayEquals(expected, actual);
    }

    private static void testStringList(List<String> expected, String pathToLoad)
    {
        // Use this to see what entries are in the list instead of only seeing that there is one.
        for (String loadKey : ConfigUtil.loadKeys(pathToLoad)) {
            final String fullKey = pathToLoad + '.' + loadKey;
            Assertions.assertTrue(AntiCheatAddition.getInstance().getConfig().contains(fullKey), "Does not exist: " + fullKey);
            Assertions.assertIterableEquals(expected, ConfigUtil.loadImmutableStringOrStringList(fullKey), "Failed for: " + fullKey);
        }
    }

    @Test
    void testEmptyStringList()
    {
        testStringList(List.of(), "Test.Empty");
    }

    @Test
    void testOneStringList()
    {
        testStringList(List.of("1"), "Test.One");
    }

    @Test
    void testTwoStringList()
    {
        testStringList(List.of("1", "2"), "Test.Two");
    }
}

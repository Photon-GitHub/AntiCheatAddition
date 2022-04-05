package de.photon.anticheataddition;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ServerVersionTest
{
    @BeforeAll
    static void mockEnvironment()
    {
        Dummy.mockEnvironment();
    }

    @Test
    void serverVersionTest()
    {
        Assertions.assertEquals(ServerVersion.MC118, ServerVersion.getActiveServerVersion());
    }

    @Test
    void serverVersionFromToTest()
    {
        Assertions.assertIterableEquals(Set.of(ServerVersion.MC18), ServerVersion.MC18.getSupVersionsTo());

        List<String> expected = Arrays.stream(ServerVersion.values())
                                      .filter(ServerVersion::isSupported)
                                      .map(ServerVersion::getVersionOutputString)
                                      .sorted()
                                      .collect(Collectors.toUnmodifiableList());

        List<String> actual = ServerVersion.MC18.getSupVersionsFrom().stream()
                                                .map(ServerVersion::getVersionOutputString)
                                                .sorted()
                                                .collect(Collectors.toUnmodifiableList());

        Assertions.assertIterableEquals(expected, actual);

        expected = Stream.of(ServerVersion.MC18, ServerVersion.MC19, ServerVersion.MC110, ServerVersion.MC111, ServerVersion.MC112)
                         .filter(ServerVersion::isSupported)
                         .map(ServerVersion::getVersionOutputString)
                         .sorted()
                         .collect(Collectors.toUnmodifiableList());

        actual = ServerVersion.MC112.getSupVersionsTo().stream()
                                    .map(ServerVersion::getVersionOutputString)
                                    .sorted()
                                    .collect(Collectors.toUnmodifiableList());

        Assertions.assertIterableEquals(expected, actual);
    }
}

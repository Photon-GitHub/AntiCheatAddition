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
    static void setup()
    {
        Dummy.mockEnvironment();
    }

    @Test
    void serverVersionTest()
    {
        Assertions.assertEquals(ServerVersion.MC118, ServerVersion.ACTIVE);
    }

    @Test
    void serverVersionFromToTest()
    {
        Assertions.assertIterableEquals(Set.of(ServerVersion.MC18), ServerVersion.MC18.getSupVersionsTo());

        List<String> expected = Arrays.stream(ServerVersion.values())
                                      .filter(ServerVersion::isSupported)
                                      .map(ServerVersion::getVersionOutputString)
                                      .sorted()
                                      .toList();

        List<String> actual = ServerVersion.MC18.getSupVersionsFrom().stream()
                                                .map(ServerVersion::getVersionOutputString)
                                                .sorted()
                                                .toList();

        Assertions.assertIterableEquals(expected, actual);

        expected = Stream.of(ServerVersion.MC18, ServerVersion.MC19, ServerVersion.MC110, ServerVersion.MC111, ServerVersion.MC112)
                         .filter(ServerVersion::isSupported)
                         .map(ServerVersion::getVersionOutputString)
                         .sorted().toList();

        actual = ServerVersion.MC112.getSupVersionsTo().stream()
                                    .map(ServerVersion::getVersionOutputString)
                                    .sorted().toList();

        Assertions.assertIterableEquals(expected, actual);
    }

    @Test
    void allSupportedVersionsTest()
    {
        final var expected = Arrays.stream(ServerVersion.values())
                                   .filter(ServerVersion::isSupported)
                                   .collect(Collectors.toUnmodifiableSet());

        Assertions.assertTrue(expected.containsAll(ServerVersion.ALL_SUPPORTED_VERSIONS), "ALL_SUPPORTED_VERSIONS contains unsupported version.");
        Assertions.assertTrue(ServerVersion.ALL_SUPPORTED_VERSIONS.containsAll(expected), "ALL_SUPPORTED_VERSIONS does not contain all supported versions.");
    }

    @Test
    void protocolToServerVersionTest()
    {
        for (ServerVersion value : ServerVersion.values()) {
            Assertions.assertFalse(value.getProtocolVersions().isEmpty(), "No protocol versions defined for ServerVersion " + value.name());
        }

        Assertions.assertEquals(ServerVersion.MC18, ServerVersion.getByProtocolVersionNumber(47).orElse(null));
        for (int i : new int[]{335, 338, 340}) Assertions.assertEquals(ServerVersion.MC112, ServerVersion.getByProtocolVersionNumber(i).orElse(null));
        for (int i : new int[]{735, 736, 751, 753, 754}) Assertions.assertEquals(ServerVersion.MC116, ServerVersion.getByProtocolVersionNumber(i).orElse(null));
        for (int i : new int[]{759, 760, 761}) Assertions.assertEquals(ServerVersion.MC119, ServerVersion.getByProtocolVersionNumber(i).orElse(null));
    }
}

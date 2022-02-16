package de.photon.aacadditionpro.protocol;

import de.photon.aacadditionpro.ServerVersion;
import de.photon.aacadditionpro.util.datastructure.Pair;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum ProtocolVersion
{
    MC18("1.8", ServerVersion.MC18, 47),
    MC19("1.9", ServerVersion.MC19, 107, 108, 109, 110),
    MC110("1.10", ServerVersion.MC110, 210),
    MC111("1.11", ServerVersion.MC111, 315, 316),
    MC112("1.12", ServerVersion.MC112, 335, 338, 340),
    MC113("1.13", ServerVersion.MC113, 393, 401, 404),
    MC114("1.14", ServerVersion.MC114, 477, 480, 485, 490, 498),
    MC115("1.15", ServerVersion.MC115, 573, 575),
    MC116("1.16", ServerVersion.MC116, 735, 736, 751, 753, 754),
    MC117("1.17", ServerVersion.MC117, 755, 756),
    MC118("1.18", ServerVersion.MC118, 757);

    private static final Map<Integer, ProtocolVersion> VERSION_NUMBER_MAP = Arrays.stream(ProtocolVersion.values())
                                                                                  // Map each ProtocolVersion to pairs of (Version Number, ProtocolVersion)
                                                                                  .flatMap(pv -> pv.getVersionNumbers().stream().map(vn -> Pair.of(vn, pv)))
                                                                                  // Create a Map from version number to ProtocolVersion.
                                                                                  .collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond));

    /**
     * The name of the {@link ProtocolVersion}. Intended to be equivalent to minecraft versions.
     * Examples: 1_8, 1_9, 1_10, etc.
     */
    private final String name;

    /**
     * What {@link ServerVersion} should be used when using this {@link ProtocolVersion}.
     */
    @NotNull
    private final ServerVersion equivalentServerVersion;

    /**
     * An immutable {@link Set} of {@link Integer}s that contains all protocol version numbers associated with this {@link ProtocolVersion}
     */
    private final Set<Integer> versionNumbers;

    ProtocolVersion(@NotNull final String name, @NotNull final ServerVersion equivalentServerVersion, @NotNull final Integer... versionNumbers)
    {
        this.name = name;
        this.equivalentServerVersion = equivalentServerVersion;
        this.versionNumbers = Set.of(versionNumbers);
    }

    /**
     * This gets the respective {@link ProtocolVersion} for a version number as returned by the {@link com.viaversion.viaversion.api.ViaAPI}
     */
    public static Optional<ProtocolVersion> getByVersionNumber(int versionNumber)
    {
        return Optional.ofNullable(VERSION_NUMBER_MAP.get(versionNumber));
    }
}

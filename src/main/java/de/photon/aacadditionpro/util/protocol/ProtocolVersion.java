package de.photon.aacadditionpro.util.protocol;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import de.photon.aacadditionpro.ServerVersion;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

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
    MC116("1.16", ServerVersion.MC116, 735, 736, 751, 753, 754);

    private static final Map<Integer, ProtocolVersion> VERSION_NUMBER_MAP;

    static {
        ImmutableMap.Builder<Integer, ProtocolVersion> builder = ImmutableMap.builder();
        for (ProtocolVersion value : ProtocolVersion.values()) {
            for (Integer versionNumber : value.versionNumbers) builder.put(versionNumber, value);
        }
        VERSION_NUMBER_MAP = builder.build();
    }

    /**
     * The name of the {@link ProtocolVersion}. Intended to be equivalent to minecraft versions.
     * Examples: 1_8, 1_9, 1_10, etc.
     */
    private final String name;

    /**
     * What {@link ServerVersion} should be used when using this {@link ProtocolVersion}.
     */
    private final ServerVersion equivalentServerVersion;

    /**
     * An immutable {@link Set} of {@link Integer}s that contains all protocol version numbers associated with this {@link ProtocolVersion}
     */
    private final Set<Integer> versionNumbers;

    ProtocolVersion(@NotNull final String name, @NotNull final ServerVersion equivalentServerVersion, @NotNull final Integer... versionNumbers)
    {
        this.name = name;
        this.equivalentServerVersion = equivalentServerVersion;
        this.versionNumbers = ImmutableSet.copyOf(versionNumbers);
    }

    /**
     * This gets the respective {@link ProtocolVersion} for a version number as returned by the {@link us.myles.ViaVersion.api.ViaAPI}.
     */
    @Nullable
    public static ProtocolVersion getByVersionNumber(int versionNumber)
    {
        // Latest ProtocolVersion as default.
        return VERSION_NUMBER_MAP.get(versionNumber);
    }
}

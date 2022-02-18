package de.photon.aacadditionpro.util.pluginmessage;

import com.google.common.base.Preconditions;
import de.photon.aacadditionpro.ServerVersion;
import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@EqualsAndHashCode(onlyExplicitlyIncluded = true, cacheStrategy = EqualsAndHashCode.CacheStrategy.LAZY)
public class LegacyMessageChannel implements MessageChannel
{
    @EqualsAndHashCode.Include @NotNull private final String legacyString;

    public LegacyMessageChannel(@NotNull String legacyString)
    {
        this.legacyString = Preconditions.checkNotNull(legacyString, "Tried to create LegacyMessageChannel with null legacyString.");
        Preconditions.checkState(ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS.contains(ServerVersion.getActiveServerVersion()), "Tried to create LegacyMessageChannel on new version.");
    }

    @Override
    public Optional<String> getChannel()
    {
        return Optional.of(legacyString);
    }
}

package de.photon.anticheataddition.util.pluginmessage;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.ServerVersion;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public record LegacyMessageChannel(@NotNull String legacyString) implements MessageChannel
{
    public LegacyMessageChannel
    {
        Preconditions.checkNotNull(legacyString, "Tried to create LegacyMessageChannel with null legacyString.");
        Preconditions.checkState(ServerVersion.containsActive(ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS), "Tried to create LegacyMessageChannel on new version.");
    }

    @Override
    public Optional<String> getChannel()
    {
        return Optional.of(legacyString);
    }
}

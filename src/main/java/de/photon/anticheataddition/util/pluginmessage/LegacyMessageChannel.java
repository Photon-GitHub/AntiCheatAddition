package de.photon.anticheataddition.util.pluginmessage;

import com.google.common.base.Preconditions;
import de.photon.anticheataddition.ServerVersion;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Value
@Getter(AccessLevel.NONE)
public class LegacyMessageChannel implements MessageChannel
{
    @NotNull String legacyString;

    public LegacyMessageChannel(@NotNull String legacyString)
    {
        this.legacyString = Preconditions.checkNotNull(legacyString, "Tried to create LegacyMessageChannel with null legacyString.");
        Preconditions.checkState(ServerVersion.containsActive(ServerVersion.LEGACY_PLUGIN_MESSAGE_VERSIONS), "Tried to create LegacyMessageChannel on new version.");
    }

    @Override
    public Optional<String> getChannel()
    {
        return Optional.of(legacyString);
    }
}
